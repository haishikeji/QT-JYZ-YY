package com.px.mgr.controller.store;

import com.alibaba.fastjson.JSONArray;
import com.github.pagehelper.PageInfo;
import com.px.db.custom.service.MainTotalService;
import com.px.db.model.bean.*;
import com.px.db.model.service.*;
import com.px.db.util.PageModel;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.db.util.SqlSortModel;
import com.px.util.EncryptionUtil;
import com.px.util.MyExistUtil;
import com.px.web.controller.BaseController;
import com.px.web.service.StoreHelpService;
import com.px.web.util.Nav.NavEnum;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.login.Authorization;
import com.px.web.util.login.LoginUtil;
import com.px.web.util.login.PermissionEnum;
import com.px.web.util.login.PermissionGroupEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RequestMapping("store")
@Controller
@Slf4j
public class StoreController extends BaseController {

    @Resource
    private StoreService storeService;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private RegionService regionService;
    @Resource
    private SettingsService settingsService;
    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private OilService oilService;
    @Resource
    private MainTotalService mainTotalService;
    @Resource
    private  UserIntegralService userIntegralService;
    @Resource
    private StoreHelpService storeHelpService;

    private PermissionGroupEnum navGroup = PermissionGroupEnum.store;

    @RequestMapping("/index.html")
    public String indexInput() {
        //获取子级菜单
        List<NavEnum> navList = NavEnum.getNavsByGroup(navGroup.name());
        if (navList.size() > 0) {
            return "redirect:" + navList.get(0).getUrl();
        } else {
            return "global/no_permission_pop";
        }
    }

    @RequestMapping("/list.html")
    @Authorization(PermissionEnum.mchStoreList)
    public String selfInput(ModelMap model) {
        List<NavEnum> navList = NavEnum.getNavsByGroup(navGroup.name());
        model.put("navList", navList);
        model.put("navGroup", navGroup.getGroupName());
        model.put("navKey", NavEnum.storeList.getKey());

        model.put("mapKey",settingsService.selectOneByProperty("code","map_qq_key").getValue());
        return "store/list";
    }

    @RequestMapping("/storeList")
    @ResponseBody
    @Authorization(PermissionEnum.mchStoreList)
    public PageModel storeList(@ModelAttribute Store store, Integer page, Integer limit, String sortField, String sortType) {
        //分页排序查询
        if (MyExistUtil.isEmptyString(sortField)) {
            sortField = Store.STATUS_FIELD;
        }
        if (MyExistUtil.isEmptyString(sortType)) {
            sortType = "asc";
        }

        AdminUser user = LoginUtil.getUserCache(AdminUser.class);

        QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();

        searches.addQuery(new QueryModel(Store.MCH_ID_PROPERTY,user.getMchId(),QueryModel.EQUALS));

        if (MyExistUtil.notEmptyString(store.getTitle())) {
            searches.addQuery(new QueryModel(Store.TITLE_PROPERTY,store.getTitle(),QueryModel.FULL_LIKE));
        }
        if (MyExistUtil.notEmptyByte(store.getStatus())){
            searches.addQuery(new QueryModel(Store.STATUS_PROPERTY,store.getStatus(),QueryModel.EQUALS));
        }

        PageInfo<Store> pageInfo = storeService.selectByQueryWithPage(searches, page, limit, new SqlSortModel(sortField, sortType), new SqlSortModel("id", false));

        return new PageModel<>(pageInfo);
    }

    @RequestMapping("/edit_store.html")
    @Authorization(PermissionEnum.mchStoreEdit)
    public String editStore(Integer id,ModelMap model){

        Store item = storeService.selectByPrimaryKey(id);
        if (item == null){
            item = new Store();
            item.setTitle("");
//            Settings password = settingsService.selectOneEqualsT(new Settings().setCode("user_default_password"));
//            if(password != null && MyExistUtil.notEmptyString(password.getValue())){
//                item.setPassword(password.getValue());
//            }
        }
        if (MyExistUtil.notEmptyString(item.getRecommendedPackages())){
            String rp = item.getRecommendedPackages();
            String s = rp.replaceAll(",", "\n");
            item.setRecommendedPackages(s);
        }
        if(MyExistUtil.notEmptyString(item.getIntegralGet())){
            List<Map>  mapList= JSONArray.parseArray(item.getIntegralGet(),Map.class);
            model.put("contentList",mapList);
        }
        List<Region> provinces = regionService.selectEqualsProperty(Region.REGION_TYPE_PROPERTY, 1);
        model.put("provinces",provinces);
        List<Region> cities = regionService.selectEqualsProperty(Region.REGION_TYPE_PROPERTY, 2);
        model.put("cities",cities);
        List<Region> areas = regionService.selectEqualsProperty(Region.REGION_TYPE_PROPERTY, 3);
        model.put("areas",areas);

        model.put("item",item);
        model.put("mapKey",settingsService.selectOneByProperty("code","map_qq_key").getValue());

        return "store/edit_store";
    }

    @RequestMapping("/saveStore")
    @ResponseBody
    @Authorization(PermissionEnum.mchStoreEdit)
    public ResponseResultUtil save(Store store){

        if (MyExistUtil.notEmptyString(store.getRecommendedPackages())){
            String rp = store.getRecommendedPackages();
            String[] split = rp.split("\n");
            if (split.length!=3){
                return ResponseResultUtil.lose("推荐套餐固定为三个数据");
            }
            for (String s : split) {
                try {
                    Double.parseDouble(s);
                } catch (NumberFormatException e) {
                   return ResponseResultUtil.lose("推荐套餐数据只能为数字");
                }
            }
            String s1 = rp.replaceAll("\n", ",");
            store.setRecommendedPackages(s1);
        }

        AdminUser user = LoginUtil.getUserCache(AdminUser.class);

        if (MyExistUtil.isEmptyString(store.getTitle())){
            return ResponseResultUtil.lose("门店名称不能为空！");
        }else{
            //查询名称是否重复
            QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
            searches.addQuery(new QueryModel("title", store.getTitle()));
            if (MyExistUtil.notEmptyInteger(store.getId())) {
                searches.addQuery(new QueryModel("id", store.getId(), QueryModel.NOT_EQUALS));
            }
            Integer count = storeService.selectCountByQuery(searches);
            if (count > 0) {
                return ResponseResultUtil.lose("名称【" + store.getTitle() + "】已存在，请修改！");
            }
        }

        if (MyExistUtil.isEmptyString(store.getOrderPrefix())){
            return ResponseResultUtil.lose("订单编号前缀不能为空！");
        }else{
            //查询名称是否重复
            QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
            searches.addQuery(new QueryModel(Store.ORDER_PREFIX_PROPERTY, store.getOrderPrefix()));
            if (MyExistUtil.notEmptyInteger(store.getId())) {
                searches.addQuery(new QueryModel("id", store.getId(), QueryModel.NOT_EQUALS));
            }
            Integer count = storeService.selectCountByQuery(searches);
            if (count > 0) {
                return ResponseResultUtil.lose("订单编号前缀【" + store.getOrderPrefix() + "】和其它门店重复，请修改！");
            }
        }
        boolean flag;

        if(MyExistUtil.isGreaterThan0Integer(store.getProvinceId())){
            store.setProvince(regionService.selectByPrimaryKey(store.getProvinceId()).getFullName());
        }
        if(MyExistUtil.isGreaterThan0Integer(store.getCityId())){
            store.setCity(regionService.selectByPrimaryKey(store.getCityId()).getFullName());
        }
        if(MyExistUtil.isGreaterThan0Integer(store.getDistrictId())){
            store.setDistrict(regionService.selectByPrimaryKey(store.getDistrictId()).getFullName());
        }

        if (MyExistUtil.isGreaterThan0Integer(store.getId())) {
            Store oldData = storeService.selectByPrimaryKey(store.getId());
            //是否重置密码
            if(MyExistUtil.notEmptyString(store.getPassword())){
                store.setPassword(EncryptionUtil.getMD5Password(store.getPassword()));
            }
            //----------如果是更新-----------
            flag = storeService.updateByPrimaryKey(store);
            if (flag) {

                AdminUser adminUser = adminUserService.selectByPrimaryKey(oldData.getAdminUserId());
                //重置密码
                if (MyExistUtil.notEmptyString(store.getPassword())){
                    adminUser.setPassword(store.getPassword());
                }
                //判断电话是否改变
                if (!(oldData.getMobile()!=null && oldData.getMobile().equals(store.getMobile()))){
                    adminUser.setMobile(store.getMobile());
                }
                //判断邮箱是否修改
                if (!(oldData.getEmail()!=null && oldData.getEmail().equals(store.getEmail()))){
                    adminUser.setEmail(store.getEmail());
                }
                if(store.getStatus()==1){
                    adminUser.setStatus((byte)1);
                }else{
                    adminUser.setStatus((byte)0);
                }

                //判断门店名称是否修改
                if (!(oldData.getTitle()!=null && oldData.getTitle().equals(store.getTitle()))){
                    storeHelpService.updateStoreName(store.getId(),store.getTitle());
                }

                adminUserService.updateByPrimaryKey(adminUser);

                //记录日志
                String[]  columnName={Store.TITLE_FIELD};
                Object[] oldValues={oldData.getTitle()};
                Object[]  newValues={store.getTitle()};
                insertFiledsModifyLog(Store.TABLE_NAME,store.getId(),oldData.getTitle(),columnName,oldValues,newValues);
            }
        }else{
            //----------如果是新增-----------
            store.setPassword(EncryptionUtil.getMD5Password(store.getPassword())).setMchId(user.getMchId());

            //判断是否存在同名登录名
            QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
            searches.addQuery(new QueryModel("type","store"));
            searches.addQuery(new QueryModel(AdminUser.USER_NAME_PROPERTY,store.getLoginName(), QueryModel.EQUALS));
            Integer count = adminUserService.selectCountByQuery(searches);
            if (count > 0) {
                return ResponseResultUtil.lose("登陆账号【" + store.getLoginName() + "】已存在，请修改！");
            }

            flag = storeService.insertOne(store.setCreateTime(new Date()).setCreateUid(user.getId()).setCreateUser(user.getUserName()));
            if (flag){
                insertAddLog(new Store(),store.getTitle(),store.getId());
                Store store1 = storeService.selectByPrimaryKey(store);
                //创建一个登录账户
                AdminUser adminUser = new AdminUser().setType("store")
                        .setStoreId(store1.getId()).setStoreName(store1.getTitle()).setMchId(store1.getMchId()).setMchName(user.getMchName())
                        .setUserName(store1.getLoginName()).setPassword(store1.getPassword())
                        .setRealName(store1.getContactName()).setNickName(store1.getContactName()).setMobile(store1.getMobile()).setEmail(store1.getEmail())
                        .setIsSuper((byte) 0).setCreateUid(LoginUtil.getUserCache(AdminUser.class).getId())
                        .setCreateTime(new Date()).setUpdateTime(new Date()).setIsStoreSuper(true);
                if(store1.getStatus()==1){
                    adminUser.setStatus((byte)1);
                }else{
                    adminUser.setStatus((byte)0);
                }
                boolean flag1 = adminUserService.insertOne(adminUser);
                if (flag1){
                    storeService.updateByPrimaryKey(store1.setAdminUserId(adminUserService.selectByPrimaryKey(adminUser).getId()));
                }
            }

        }
        return flag?ResponseResultUtil.success():ResponseResultUtil.lose("保存失败！");
    }

    @RequestMapping("/view.html")
    @Authorization(PermissionEnum.mchStoreList)
    public String viewInput(ModelMap model,Integer id) {

        Store store = storeService.selectByPrimaryKey(id);
        model.put("item",store);

        return "store/view";
    }

    @RequestMapping("/deleteStore")
    @ResponseBody
    @Authorization(PermissionEnum.mchStoreEdit)
    public ResponseResultUtil deleteStore(Integer id){
        Store item = storeService.selectByPrimaryKey(id);
        if (item == null){
            return ResponseResultUtil.lose("门店不存在或已删除!");
        }
        int count1 = orderInfoService.selectCountEqualsProperty(OrderInfo.STORE_ID_PROPERTY, id);
        if (count1>0){
            return ResponseResultUtil.lose("门店下存在订单，无法删除！");
        }
        int count2 = oilService.selectCountEqualsProperty(Oil.STORE_ID_PROPERTY, id);
        if (count2>0){
            return  ResponseResultUtil.lose("门店下存在油号，无法删除！");
        }
        List<UserIntegral> userIntegralList=userIntegralService.selectEqualsT(new UserIntegral().setStoreId(id));
        if(userIntegralList.size()>0){
            return  ResponseResultUtil.lose("门店下存在用户有加油金，无法删除！");
        }
        Store store = storeService.selectByPrimaryKey(id);
        boolean flag = storeService.deleteByPrimaryKey(store);
        if (flag){
            //删除登录账户
            adminUserService.deleteByPrimaryKey(store.getAdminUserId());
            //记录日志
            insertDelLog(new Store(),item.getTitle(),item.getId());
        }
        return flag?ResponseResultUtil.success() : ResponseResultUtil.lose("删除失败");
    }

    @RequestMapping("/viewAddress.html")
    @Authorization(PermissionEnum.mchStoreEdit)
    public String viewAddressInput(ModelMap model,Integer id) {

        Store store = storeService.selectByPrimaryKey(id);
        model.put("item",store);

        return "store/view_address";
    }

    @RequestMapping("/modifyAddress.html")
    @Authorization(PermissionEnum.mchStoreEdit)
    public String modifyAddressInput(ModelMap model) {
        model.put("mapKey",settingsService.selectOneByProperty("code","map_qq_key").getValue());
        return "store/modify_address";
    }


    @RequestMapping("/distribution.html")
    @Authorization(PermissionEnum.mchStoreList)
    public String Input(ModelMap model) {
        List<NavEnum> navList = NavEnum.getNavsByGroup(navGroup.name());
        model.put("navList", navList);
        model.put("navGroup", navGroup.getGroupName());
        model.put("navKey", NavEnum.mchStoreDistribution.getKey());

        return "store/distribution";
    }

    @RequestMapping("/getStoreDistribution")
    @ResponseBody
    public List<Map<String,Object>> getStoreDistribution(Integer provinceId) {
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);

        List<Map<String,Object>> list = mainTotalService.getSumStoreGroup(new Store().setProvinceId(provinceId).setMchId(user.getMchId()),"city");
        return list;
    }

    @RequestMapping("/statistics")
    @ResponseBody
    public ModelMap statistics() {
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);

        ModelMap map = new ModelMap();

        QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
        searches.addQuery(new QueryModel(Store.MCH_ID_PROPERTY,user.getMchId()));
        int storeCount = storeService.selectCountByQuery(searches);

        //订单统计
        map.put("store",mainTotalService.getSumStoreGroup(new Store().setMchId(user.getMchId()),"province"));
        map.put("storeCount",storeCount);

        return map;
    }

}

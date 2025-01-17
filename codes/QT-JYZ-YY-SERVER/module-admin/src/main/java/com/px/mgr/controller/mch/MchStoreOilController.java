package com.px.mgr.controller.mch;

import com.github.pagehelper.PageInfo;
import com.px.db.model.bean.*;
import com.px.db.model.service.*;
import com.px.db.util.PageModel;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.db.util.SqlSortModel;
import com.px.web.controller.BaseController;
import com.px.web.service.OilHelpService;
import com.px.web.util.Nav.NavEnum;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.login.Authorization;
import com.px.web.util.login.LoginUtil;
import com.px.web.util.login.PermissionEnum;
import com.px.web.util.login.PermissionGroupEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 入驻商门店油价管理
 */
@Slf4j
@RequestMapping("/store")
@Controller
public class MchStoreOilController extends BaseController {
    @Resource
    private OilService oilService;
    @Resource
    private OilTypeService oilTypeService;
    @Resource
    private OilNumberService oilNumberService;
    @Resource
    private StoreService storeService;
    @Resource
    private QrcodeListService qrcodeListService;
    @Resource
    private MchService mchService;
    @Resource
    private OilHelpService oilHelpService;

    /**
     * 入驻商门店油价列表页面
     *
     * @param map
     * @return
     */
    @Authorization(PermissionEnum.mchStoreOil)
    @RequestMapping("/oil.html")
    public String listInput(ModelMap map) {
        List<NavEnum> navEnumList = NavEnum.getBtnByGroup(PermissionGroupEnum.mch.name());
        List<NavEnum> navEnums = new ArrayList<>();
        for (NavEnum nav : navEnumList) {
            if (nav.getKey().equals("mchStoreOilAdd")) {
                navEnums.add(nav);
            }
        }
        //公共按钮
        map.put("btnList", navEnums);
        //列表操作选项
        map.put("actions", NavEnum.getActionByGroup(PermissionGroupEnum.mch.name()));
        //油品下拉选
        List<OilType> oilTypeList = oilTypeService.selectEqualsT(new OilType().setStatus((byte) 1));
        map.put("oilTypeList", oilTypeList);
        //获取油号下拉选
        List<OilNumber> oilNumberList = oilNumberService.selectAll();
        for (OilNumber o : oilNumberList) {
            o.setOilNumber(o.getOilNumber() + "（" + o.getOilTypeName() + "）");
        }
        map.put("oilNumberList", oilNumberList);
        //获取所有入驻商下拉选
        List<Mch> mchList = mchService.selectAll();
        map.put("mchList", mchList);
        //获取所有入驻商门店下拉选
        QueryHelper.SimpleQueryHandler handler = QueryHelper.startSimpleQuery();
        handler.addQuery(new QueryHelper.OrQueryModel(Oil.MCH_ID_PROPERTY, 0, QueryModel.NOT_EQUALS));
        List<Store> storeList = storeService.selectByQuery(handler);
        map.put("storeList", storeList);
        return "mch/mch_store_oil_list";
    }


    /**
     * 获取所有入驻商油价列表
     *
     * @param page
     * @param limit
     * @return
     */
    @Authorization(PermissionEnum.mchStoreOil)
    @ResponseBody
    @RequestMapping("/oillist")
    public PageModel list(Integer mchId, Integer storeId, Integer oilTypeId, Integer oilNumberId, Integer isSale, Integer page, Integer limit) {
        QueryHelper.SimpleQueryHandler handler = QueryHelper.startSimpleQuery();
        handler.addQuery(new QueryHelper.OrQueryModel(Oil.MCH_ID_PROPERTY, 0, QueryModel.NOT_EQUALS));
        //入驻商id
        if (null != mchId) {
            handler.addQuery(new QueryHelper.OrQueryModel(Oil.MCH_ID_PROPERTY, mchId, QueryModel.EQUALS));
        }
        //门店id
        if (null != storeId) {
            handler.addQuery(new QueryHelper.OrQueryModel(Oil.STORE_ID_PROPERTY, storeId, QueryModel.EQUALS));
        }
        //油品id
        if (null != oilTypeId) {
            handler.addQuery(new QueryHelper.OrQueryModel(Oil.OIL_TYPE_ID_PROPERTY, oilTypeId, QueryModel.EQUALS));
        }
        //油号id
        if (null != oilNumberId) {
            handler.addQuery(new QueryHelper.OrQueryModel(Oil.OIL_NUMBER_ID_PROPERTY, oilNumberId, QueryModel.EQUALS));
        }
        //状态
        if (null != isSale) {
            handler.addQuery(new QueryHelper.OrQueryModel(Oil.IS_SALE_PROPERTY, isSale, QueryModel.EQUALS));
        }
        PageInfo<Oil> pageInfo = oilService.selectByQueryWithPage(handler, page, limit, new SqlSortModel("id", true));
        Integer count = oilService.selectCountByQuery(handler);
        PageModel pageModel = new PageModel();
        pageModel.setList(pageInfo.getList()).setRel(true).setCount(count);
        return pageModel;
    }


    /**
     * 编辑/新增油价页面
     *
     * @param map
     * @param id       油价id
     * @param openType 页面打开类型
     * @return
     */
    @Authorization(PermissionEnum.mchStoreOilEdit)
    @RequestMapping("/editOil.html")
    public String editInput(ModelMap map, Integer id, String openType) {
        Oil oil = new Oil();
        if (null != id) {
            oil = oilService.selectByPrimaryKey(id);
        }
        map.put("item", oil);
        //油号下拉选
        List<OilNumber> oilNumberList = oilNumberService.selectEqualsT(new OilNumber().setStatus((byte) 1));
        for (OilNumber o : oilNumberList) {
            o.setOilNumber(o.getOilNumber() + "（" + o.getOilTypeName() + "）");
        }
        map.put("oilNumberList", oilNumberList);
        //油枪列表
        List<String> refuelGunsList = getRefuelGunsList(oil.getRefuelGuns());
        map.put("refuelGunsList", refuelGunsList);
        //获取所有入驻商门店下拉选
        QueryHelper.SimpleQueryHandler handler = QueryHelper.startSimpleQuery();
        handler.addQuery(new QueryHelper.OrQueryModel(Store.MCH_ID_PROPERTY, 0, QueryModel.NOT_EQUALS));
        List<Store> storeList = storeService.selectByQuery(handler);
        for (Store s : storeList) {
            s.setTitle(s.getTitle() + "（" + s.getMchName() + "）");
        }
        map.put("storeList", storeList);
        map.put("openType", openType);
        return "mch/mch_store_oil_edit";
    }


    /**
     * 编辑/新增保存
     *
     * @param oil      油价id
     * @param openType 操作类型
     * @return
     */
    @Authorization(PermissionEnum.mchStoreOilEdit)
    @ResponseBody
    @RequestMapping("/editSaveOil")
    public ResponseResultUtil editSave(Oil oil, String openType) {
        //非空判断
        if (null == openType || "".equals(openType)) {
            return ResponseResultUtil.lose("操作失败");
        }
        //油号id
        if (null == oil.getOilNumberId()) {
            return ResponseResultUtil.lose("油号不能为空");
        }
        //门店id
        if (null == oil.getStoreId()) {
            return ResponseResultUtil.lose("门店不能为空");
        }
        //价格
        if (null == oil.getPrice()) {
            return ResponseResultUtil.lose("价格不能为空");
        }
        //上下架状态
        if (null == oil.getIsSale()) {
            return ResponseResultUtil.lose("状态不能为空");
        }
        //价格
        if (oil.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseResultUtil.lose("价格必须大于0");
        }
        //判断油号是否存在
        OilNumber oilNumber = oilNumberService.selectByPrimaryKey(oil.getOilNumberId());
        if (null == oilNumber) {
            return ResponseResultUtil.lose("操作失败，油号不存在或已被删除");
        }
        //判断油号状态
        if (oilNumber.getStatus() == 0) {
            return ResponseResultUtil.lose("操作失败，油号【" + oilNumber.getOilNumber() + "】状态为禁用");
        }
        //判断油号对应的油品是否存在
        OilType oilType = oilTypeService.selectByPrimaryKey(oilNumber.getOilTypeId());
        if (null == oilType) {
            return ResponseResultUtil.lose("操作失败，油号【" + oilNumber.getOilNumber() + "】对应的油品【" + oilNumber.getOilTypeName() + "】不存在或已被删除");
        }
        //判断油品状态
        if (oilType.getStatus() == 0) {
            return ResponseResultUtil.lose("操作失败，油号【" + oilNumber.getOilNumber() + "】对应的油品【" + oilNumber.getOilTypeName() + "】状态为禁用");
        }
        //判断门店是否存在
        Store store = storeService.selectByPrimaryKey(oil.getStoreId());
        if (null == store) {
            return ResponseResultUtil.lose("操作失败，门店不存在或已被删除");
        }
        //判断输入的油枪是否有重复
        if (null!=oil.getRefuelGuns()&&!"".equals(oil.getRefuelGuns())){
            if (oil.getRefuelGuns().contains(",")){
                String strArr[] = oil.getRefuelGuns().split(",");
                if (hasDuplicate(strArr)){
                    return ResponseResultUtil.lose("操作失败，输入的油枪号不能重复");
                }
            }
        }
        /**---------------------------------------------------------------以下为新增-----------------------------------------------------------------------------**/
        if (openType.equals("add")) {
            //判断该门店是否已存在该油价
            List<Oil> oilExistsList = oilService.selectEqualsT(new Oil().setStoreId(oil.getStoreId()).setOilNumberId(oil.getOilNumberId()));
            if (oilExistsList.size() > 0) {
                return ResponseResultUtil.lose("操作失败，油号【" + oilNumber.getOilNumber() + "】的油价已存在");
            }
            /**-----------------------------------处理数据-----------------------------------**/
            //处理数据：油品id，油品名称，油号名称，入驻商id，入驻商名称，门店名称，创建人id，创建人名称
            AdminUser adminUser = LoginUtil.getUserCache(AdminUser.class);
            //实时获取门店信息
            Mch mch = new Mch();
            if (store.getMchId() == 0) {
                //平台自营门店
                mch.setId(0).setTitle("平台自营");
            } else {
                //入驻商门店
                mch = mchService.selectByPrimaryKey(store.getMchId());
            }
            oil.setOilTypeId(oilType.getId()).
                    setOilTypeName(oilType.getTitle()).
                    setOilNumber(oilNumber.getOilNumber()).
                    setMchId(store.getMchId()).
                    setMchName(mch.getTitle()).
                    setStoreName(store.getTitle()).
                    setCreateUid(adminUser.getId()).
                    setCreateUser(adminUser.getRealName());
            /**-----------------------------------处理数据结束-----------------------------------**/
            //插入数据
            boolean insertFlag = oilService.insertOne(oil);
            if (insertFlag) {
                //插入日志
                boolean insertLog = insertAddLog(new Oil(), "门店：" + oil.getStoreName() + "，油号：" + oil.getOilNumber() + "，价格：" + oil.getPrice() + "，加油枪编号：" + oil.getRefuelGuns(), oil.getId());
                if (insertLog) {
                    return ResponseResultUtil.success();
                }
            }
        }
        /**---------------------------------------------------------------以下为编辑-----------------------------------------------------------------------------**/
        else if (openType.equals("edit")) {
            //判空
            if (null == oil.getId()) {
                return ResponseResultUtil.lose("操作失败");
            }
            //判断数据是否存在
            Oil oldOil = oilService.selectByPrimaryKey(oil.getId());
            if (null == oldOil) {
                return ResponseResultUtil.lose("操作失败，油价不存在或已被删除");
            }
            //当修改门店油号时，判断该门店是否已存在该油价
            if ((oil.getStoreId() == oldOil.getStoreId()) && (oil.getOilNumberId() != oldOil.getOilNumberId())) {
                List<Oil> oilExistsList = oilService.selectEqualsT(new Oil().setStoreId(oil.getStoreId()).setOilNumberId(oil.getOilNumberId()));
                if (oilExistsList.size() > 0) {
                    return ResponseResultUtil.lose("操作失败，【" + oilNumber.getOilNumber() + "】油价已存在");
                }
            }
            /**-----------------------------------处理数据-----------------------------------**/
            //处理数据：油品id，油品名称，油号名称，入驻商id，入驻商名称，门店名称
            //实时获取门店信息
            Mch mch = new Mch();
            if (store.getMchId() == 0) {
                //平台自营门店
                mch.setId(0).setTitle("平台自营");
            } else {
                //入驻商门店
                mch = mchService.selectByPrimaryKey(store.getMchId());
            }
            oil.setOilTypeId(oilType.getId()).
                    setOilTypeName(oilType.getTitle()).
                    setOilNumber(oilNumber.getOilNumber()).
                    setMchId(store.getMchId()).
                    setMchName(mch.getTitle()).
                    setStoreName(store.getTitle());
            /**-----------------------------------处理数据结束-----------------------------------**/
            //修改数据
            oilService.updateByPrimaryKey(oil);
            //插入日志
            String[] columnName = {Oil.OIL_TYPE_ID_PROPERTY, Oil.OIL_TYPE_NAME_PROPERTY, Oil.OIL_NUMBER_ID_PROPERTY, Oil.OIL_NUMBER_PROPERTY,
                    Oil.MCH_ID_PROPERTY, Oil.MCH_NAME_PROPERTY, Oil.STORE_ID_PROPERTY, Oil.STORE_NAME_PROPERTY, Oil.PRICE_PROPERTY,
                    Oil.REFUEL_GUNS_PROPERTY, Oil.REMARK_PROPERTY, Oil.SORT_ORDER_PROPERTY, Oil.IS_SALE_PROPERTY};

            Object[] oldValues = {oldOil.getOilTypeId(), oldOil.getOilTypeName(), oldOil.getOilNumberId(), oldOil.getOilNumber(),
                    oldOil.getMchId(), oldOil.getMchName(), oldOil.getStoreId(), oldOil.getStoreName(), oldOil.getPrice(),
                    oldOil.getRefuelGuns(), oldOil.getRemark(), oldOil.getSortOrder(), oldOil.getIsSale()};

            Object[] newValues = {oil.getOilTypeId(), oil.getOilTypeName(), oil.getOilNumberId(), oil.getOilNumber(),
                    oil.getMchId(), oil.getMchName(), oil.getStoreId(), oil.getStoreName(), oil.getPrice(),
                    oil.getRefuelGuns(), oil.getRemark(), oil.getSortOrder(), oil.getIsSale()};

            insertFiledsModifyLog(OilType.TABLE_NAME, oilType.getId(), oldOil.getOilNumber(), columnName, oldValues, newValues);
            return ResponseResultUtil.success();
        }
        return ResponseResultUtil.lose("操作失败");
    }


    /**
     * 修改状态
     *
     * @param id     油价id
     * @param status 状态
     * @return
     */
    @Authorization(PermissionEnum.mchStoreOilEdit)
    @ResponseBody
    @RequestMapping("/changeStatusOil")
    public ResponseResultUtil changeStatus(Integer id, Integer status) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("操作失败");
        }
        if (null == status) {
            return ResponseResultUtil.lose("操作失败");
        }
        //判断数据是否存在
        Oil oil = oilService.selectByPrimaryKey(id);
        if (null == oil) {
            return ResponseResultUtil.lose("操作失败,数据不存在或已被删除");
        }
        //修改状态
        boolean isSale = false;
        if (status == 1) {
            isSale = true;
        }
        boolean flag = oilService.updateByPrimaryKey(new Oil().setId(id).setIsSale(isSale));
        //插入日志
        if (flag) {
            boolean logFlag = insertOneFieldModifyLog(Oil.TABLE_NAME, Oil.IS_SALE_PROPERTY, id, "油号：" + oil.getOilNumber() + "，状态：" + oil.getIsSale(), oil.getIsSale(), status);
            if (logFlag) {
                return ResponseResultUtil.success();
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    /**
     * 删除油价
     *
     * @param id 油价id
     * @return
     */
    @Authorization(PermissionEnum.mchStoreOilEdit)
    @ResponseBody
    @RequestMapping("/delOil")
    public ResponseResultUtil del(Integer id) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("操作失败");
        }
        //判断数据是否存在
        Oil oil = oilService.selectByPrimaryKey(id);
        if (null == oil) {
            return ResponseResultUtil.lose("操作失败，数据不存在或已被删除!");
        }
        //删除油价
        boolean delFlag = oilService.deleteByPrimaryKey(id);
        if (delFlag) {
            //插入日志
            boolean delLog = insertDelLog(new Oil(), "油价id：" + oil.getId() + "，油号：" + oil.getOilNumber() + "，价格：" + oil.getPrice(), oil.getId());
            if (delLog) {
                return ResponseResultUtil.success();
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    /**
     * 查看详情
     *
     * @param map
     * @param id  油价id
     * @return
     */
    @Authorization(PermissionEnum.mchStoreOil)
    @RequestMapping("/viewOil.html")
    public String view(ModelMap map, Integer id) {
        Oil oil = new Oil();
        if (null != id) {
            oil = oilService.selectByPrimaryKey(id);
        }
        map.put("item", oil);
        //油枪列表
        List<String> refuelGunsList = new ArrayList<>();
        if (null != oil.getRefuelGuns() && !oil.getRefuelGuns().equals("")) {
            if (oil.getRefuelGuns().contains(",")) {
                String[] gunStr = oil.getRefuelGuns().split(",");
                for (String g : gunStr) {
                    refuelGunsList.add(g);
                }
            } else {
                refuelGunsList.add(oil.getRefuelGuns());
            }
        }
        map.put("refuelGunsList", refuelGunsList);
        //实时获取门店名称
        map.put("storeName", storeService.selectByPrimaryKey(oil.getStoreId()).getTitle());
        return "mch/mch_store_oil_view";
    }


    /**
     * 油枪二维码
     * @param map
     * @return
     */
    @RequestMapping("/qrcode.html")
    @Authorization(PermissionEnum.mchStoreOilQrcode)
    public String numberInput(ModelMap map,Integer id) {
        Oil oil = oilService.selectByPrimaryKey(id);
        map.put("oil", oil);
        //油枪列表
        List<String>refuelGunsList = getRefuelGunsList(oil.getRefuelGuns());
        map.put("refuelGunsList", refuelGunsList);
        //二维码种类列表
        List<QrcodeList>qrcodeLists = qrcodeListService.selectEqualsT(new QrcodeList().setType((byte)0).setCategroy("oil_gun").setIsShow(true)
                ,new SqlSortModel(QrcodeList.LENGTH_PROPERTY));
        map.put("qrcodeLists", qrcodeLists);
        map.put("qrcodeCount", qrcodeLists.size());
        map.put("qrcodeRemark", oilHelpService.getOilQrcodeRemark());
        return "oil/qrcode";
    }

    /**
     * 生成并下载油价二维码
     * @param id  油价id
     * @param gun  油枪编号
     * @param size  二维码尺寸
     * @param title  附加备注
     * @return
     */
    @ResponseBody
    @RequestMapping("/downloadQrcode")
    public ResponseResultUtil downloadQrcode(Integer id,String gun,Integer size,String title) {
        return oilHelpService.downloadOilQrcode(id,gun,size,title,"admin");
    }


    /**
     * 获取油枪列表
     *
     * @param refuelGuns 油枪
     * @return
     */
    public List<String> getRefuelGunsList(String refuelGuns) {
        List<String> refuelGunsList = new ArrayList<>();
        if ((null != refuelGuns) && (!refuelGuns.equals(""))) {
            if (refuelGuns.contains(",")) {
                String[] gunStr = refuelGuns.split(",");
                for (String g : gunStr) {
                    refuelGunsList.add(g);
                }
            } else {
                refuelGunsList.add(refuelGuns);
            }
        }
        return refuelGunsList;
    }

    private boolean hasDuplicate(String[] objArray){
        List<String> recordList = new ArrayList<>();
        for (int i = 0; i < objArray.length; i++){
            //如果不存在，则添加到列表中
            if (!recordList.contains(objArray[i])){
                recordList.add(objArray[i]);
            }else {
                //如果存在重复值，则直接跳出，返回true，表示当前图层时离散值图层
                return true;
            }
        }
        return false;
    }
}

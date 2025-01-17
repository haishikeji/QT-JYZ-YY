package com.px.mgr.controller.discount;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.px.db.custom.service.DiscountHelpService;
import com.px.db.model.bean.*;
import com.px.db.model.service.*;
import com.px.db.util.PageModel;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.db.util.SqlSortModel;
import com.px.util.DateUtil;
import com.px.util.MyExistUtil;
import com.px.web.controller.BaseController;
import com.px.web.service.DiscountStatusService;
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
import java.util.*;


/**
 * 入驻商充值优惠
 */
@Slf4j
@RequestMapping("/discount")
@Controller
public class RechargeDiscountController extends BaseController {


    @Resource
    private OilService oilService;
    @Resource
    private DiscountService discountService;
    @Resource
    private OilNumberService oilNumberService;
    @Resource
    private StoreService storeService;
    @Resource
    private DiscountHelpService discountHelpService;
    @Resource
    private MchService mchService;
    @Resource
    private DiscountStatusService discountStatusService;

    /**
     * 充值优惠页面
     *
     * @param map
     * @return
     */
    @Authorization(PermissionEnum.rechargeDiscountList)
    @RequestMapping("/recharge.html")
    public String listInput(ModelMap map) {
        List<NavEnum> navEnumList = NavEnum.getBtnByGroup(PermissionGroupEnum.activity.name());
        List<NavEnum> navEnums = new ArrayList<>();
        for (NavEnum nav : navEnumList) {
            if (nav.getKey().equals("rechargeDiscountAdd")) {
                navEnums.add(nav);
            }
        }
        //公共按钮
        map.put("btnList", navEnums);
        //列表操作选项
        map.put("actions", NavEnum.getActionByGroup(PermissionGroupEnum.activity.name()));
        return "discount/recharge_list";
    }


    /**
     * 该入驻商的充值优惠列表
     *
     * @param status      状态
     * @param title       标题
     * @param beginTime   开始一时间
     * @param endTime     结束时间
     * @param page
     * @param limit
     * @return
     */
    @Authorization(PermissionEnum.rechargeDiscountList)
    @ResponseBody
    @RequestMapping("/rechargelist")
    public PageModel rechargelist(Integer status, String title, Date beginTime, Date endTime, Integer page, Integer limit) {
        //实时刷新活动状态
        discountStatusService.updateDiscountStatus();
        QueryHelper.OrQueryHandler handler = QueryHelper.startOrQuery();
        QueryHelper.OrQueryModelList orQueryModelList = new QueryHelper.OrQueryModelList();
        //入驻商id
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);
        Integer mchId = user.getMchId();
        orQueryModelList.addAndQuery(new QueryHelper.OrQueryModel(Discount.MCH_ID_PROPERTY, mchId, QueryModel.EQUALS));
        //充值优惠
        orQueryModelList.addAndQuery(new QueryHelper.OrQueryModel(Discount.TARGET_PROPERTY, "gas_card", QueryModel.EQUALS));
        //状态
        if (null != status) {
            orQueryModelList.addAndQuery(new QueryHelper.OrQueryModel(Discount.STATUS_PROPERTY, status, QueryModel.EQUALS));
        }
        //标题
        if (null != title && !"".equals(title)) {
            orQueryModelList.addAndQuery(new QueryHelper.OrQueryModel(Discount.TITLE_PROPERTY, title, QueryModel.FULL_LIKE));
        }
        //开始时间
        if (null != beginTime && !"".equals(beginTime)) {
            String beginDate = DateUtil.FORMART.date2String(beginTime, "yyyy-MM-dd") + " 00:00:00";
            orQueryModelList.addAndQuery(new QueryHelper.OrQueryModel(Discount.BEGIN_TIME_PROPERTY, beginDate, QueryModel.GREATER_EQUALS));
        }
        //结束日期
        if (null != endTime && !"".equals(endTime)) {
            String endDate = DateUtil.FORMART.date2String(endTime, "yyyy-MM-dd") + " 23:59:59";
            orQueryModelList.addAndQuery(new QueryHelper.OrQueryModel(Discount.END_TIME_PROPERTY, endDate, QueryModel.LESS_EQUALS));
        }
        if (orQueryModelList.size() > 0) {
            handler.addOrList(orQueryModelList);
        }
        PageInfo<Discount> pageInfo = discountService.selectOrByQueryWithPage(handler, page, limit, new SqlSortModel("status", false));
        pageInfo.setList(getDiscountContent(pageInfo.getList()));
        return new PageModel<>(pageInfo);
    }


    /**
     * 编辑/新增充值优惠页面
     *
     * @param map
     * @param id       优惠活动id
     * @param openType
     * @return
     */
    @Authorization(PermissionEnum.rechargeDiscountEdit)
    @RequestMapping("/rechargeEdit.html")
    public String editInput(ModelMap map, Integer id, String openType) {
        Discount discount = new Discount();
        discount.setType("3");
        discount.setAbleOilNumber("all");
        if (null != id) {
            discount = discountService.selectByPrimaryKey(id);
        }
        if (openType.equals("copy")) {
            discount.setTitle(discount.getTitle() + "（复制）");
        }
        //优惠类型
        String conditionTitle = "";
        String conditionUnit = "";
        String discountTitle = "";
        String discountUnit = "";
        if (discount.getType() != null) {
            if (discount.getType().equals("3")) {
                conditionTitle = "充值每满";
                conditionUnit = "元";
                discountTitle = "赠";
                discountUnit = "元";
            }
        }
        Map<String, Object> content = new HashMap<>();
        if (discount.getContent() != null) {
            List<Map<String,Object>> contentList = (List<Map<String, Object>>) JSON.parse(discount.getContent());
            if (contentList.size()>0){
                content = contentList.get(0);
            }
        }
        map.put("conditionTitle", conditionTitle);
        map.put("conditionUnit", conditionUnit);
        map.put("discountTitle", discountTitle);
        map.put("discountUnit", discountUnit);
        map.put("content", content);
        map.put("openType", openType);
        map.put("item", discount);
        return "discount/recharge_edit";
    }


    /**
     * 编辑/新增充值优惠保存
     *
     * @param discount
     * @param openType
     * @return
     */
    @Authorization(PermissionEnum.rechargeDiscountEdit)
    @ResponseBody
    @RequestMapping("/rechargeEditSave")
    public ResponseResultUtil editSave(Discount discount, String openType) {
        //非空判断
        if (null == openType || "".equals(openType)) {
            return ResponseResultUtil.lose("操作失败");
        }
        //优惠名称
        if (null == discount.getTitle() || discount.getTitle().equals("")) {
            return ResponseResultUtil.lose("优惠名称不能为空");
        }
        //开始时间
        if (null == discount.getBeginTime()) {
            return ResponseResultUtil.lose("开始时间不能为空");
        }
        //结束时间
        if (null == discount.getEndTime()) {
            return ResponseResultUtil.lose("结束时间不能为空");
        }
        //优惠类型
        if (null == discount.getType() || discount.getType().equals("") || null == discount.getContent() || discount.getContent().equals("")) {
            return ResponseResultUtil.lose("优惠类型不能为空");
        }
        /**---------------------------------------------------------以下处理数据-------------------------------------------------------------------------**/
        //状态
        Long nowTime = new Date().getTime();
        if (nowTime < discount.getBeginTime().getTime()) {
            //未开始
            discount.setStatus((byte) 2);
        } else if ((nowTime >= discount.getBeginTime().getTime()) && (nowTime <= discount.getEndTime().getTime())) {
            //生效中
            discount.setStatus((byte) 3);
        } else if (nowTime > discount.getEndTime().getTime()) {
            //已结束
            discount.setStatus((byte) 1);
        }
        //优惠
        discount.setTarget("gas_card");
        /**--------------------------------------------------------------以下为新增---------------------------------------------------------------------**/
        if (openType.equals("add")) {
            //创建入驻商id,入驻商名称
            AdminUser user = LoginUtil.getUserCache(AdminUser.class);
            Integer mchId = user.getMchId();
            Mch mch = mchService.selectByPrimaryKey(mchId);
            discount.setMchId(mchId).setMchName(mch.getTitle());
            //指定入驻商
            discount.setAbleMch("appoint").setAbleMchIds(String.valueOf(mchId)).setAbleMchNames(mch.getTitle());
            //指定门店
            discount.setAbleStore("all").setAbleStoreIds(null).setAbleStoreNames(null);
            //创建者
            discount.setCreateUid(user.getId()).setCreateUser(user.getRealName());
            //插入数据
            boolean insertFlag = discountService.insertOne(discount);
            if (insertFlag) {
                //插入日志
                boolean insertLog = insertAddLog(new Discount(), "优惠标题：" + discount.getTitle() + "，创建入驻商id：" + discount.getMchId() + "，创建入驻商名称：" + discount.getMchName(), discount.getId());
                if (insertLog) {
                    return ResponseResultUtil.success();
                }
            }
        }
        /**--------------------------------------------------------------以下为编辑---------------------------------------------------------------------------**/
        else if (openType.equals("edit")) {
            //判空
            if (null == discount.getId()) {
                return ResponseResultUtil.lose("操作失败");
            }
            //判断数据是否存在
            Discount oldDiscount = discountService.selectByPrimaryKey(discount.getId());
            if (null == oldDiscount) {
                return ResponseResultUtil.lose("操作失败，数据不存在或已被删除");
            }
            //判判断是否重复
            //处理数据
            //修改数据
            discountService.updateByPrimaryKey(discount);
            //插入日志
            insertUpdateLog(new Discount(),"优惠活动id："+discount.getId() +"，优惠活动标签："+discount.getTitle()+"，优惠活动类型："+discount.getType(),discount.getId());
            return ResponseResultUtil.success();
        }
        /**--------------------------------------------------------------以下为复制---------------------------------------------------------------------------**/
        else if (openType.equals("copy")) {
            //id
            discount.setId(null);
            //创建入驻商id,入驻商名称
            AdminUser user = LoginUtil.getUserCache(AdminUser.class);
            Integer mchId = user.getMchId();
            Mch mch = mchService.selectByPrimaryKey(mchId);
            discount.setMchId(mchId).setMchName(mch.getTitle());
            //指定入驻商
            discount.setAbleMch("appoint").setAbleMchIds(String.valueOf(mchId)).setAbleMchNames(mch.getTitle());
            //指定门店
            discount.setAbleStore("all").setAbleStoreIds(null).setAbleStoreNames(null);
            //创建者
            discount.setCreateUid(user.getId()).setCreateUser(user.getRealName());
            //插入数据
            boolean insertFlag = discountService.insertOne(discount);
            if (insertFlag) {
                //插入日志
                boolean insertLog = insertAddLog(new Discount(), "优惠标题：" + discount.getTitle() + "，创建入驻商id：" + discount.getMchId() + "，创建入驻商名称：" + discount.getMchName(), discount.getId());
                if (insertLog) {
                    return ResponseResultUtil.success();
                }
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    /**
     * 充值优惠上下架
     *
     * @param id   优惠活动id
     * @param type 操作类型
     * @return
     */
    @Authorization(PermissionEnum.rechargeDiscountUpDown)
    @ResponseBody
    @RequestMapping("/rechargeUpOrDown")
    public ResponseResultUtil upOrDown(Integer id, String type) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("操作失败");
        }
        if (null == type || type.equals("")) {
            return ResponseResultUtil.lose("操作失败");
        }
        //判断数据是否存在
        Discount discount = discountService.selectByPrimaryKey(id);
        if (null == discount) {
            return ResponseResultUtil.lose("操作失败,数据不存在或已被删除");
        }
        //操作类型
        Integer status = null;
        if (type.equals("up") && discount.getStatus() == 0) {
            //改上架
            //处理数据
            Long nowTime = new Date().getTime();
            if (nowTime < discount.getBeginTime().getTime()) {
                status = 2;
            } else if ((discount.getBeginTime().getTime() <= nowTime) && (discount.getEndTime().getTime() >= nowTime)) {
                status = 3;
            } else if (discount.getEndTime().getTime() < nowTime) {
                status = 1;
            }
        } else if (type.equals("down")) {
            //改下架
            //处理数据
            status = 0;
        }
        //修改状态
        boolean flag = discountService.updateByPrimaryKey(new Discount().setId(id).setStatus(status.byteValue()));

        //插入日志
        if (flag) {
            boolean logFlag = insertOneFieldModifyLog(Discount.TABLE_NAME, Discount.STATUS_PROPERTY, id, "优惠券id：" + discount.getTitle() + "，状态：" + status, discount.getStatus(), status);
            if (logFlag) {
                return ResponseResultUtil.success();
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    /**
     * 充值优惠删除
     *
     * @param id 优惠活动id
     * @return
     */
    @Authorization(PermissionEnum.rechargeDiscountEdit)
    @ResponseBody
    @RequestMapping("/rechargeDel")
    public ResponseResultUtil del(Integer id) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("操作失败");
        }
        //判断数据是否存在
        Discount discount = discountService.selectByPrimaryKey(id);
        if (null == discount) {
            return ResponseResultUtil.lose("操作失败，数据不存在或已被删除!");
        }
        //删除优惠活动
        boolean delFlag = discountService.deleteByPrimaryKey(id);
        if (delFlag) {
            //插入日志
            boolean delLog = insertDelLog(new Discount(), "优惠活动id：" + discount.getId() + "，优惠活动：" + discount.getTitle(), discount.getId());
            if (delLog) {
                return ResponseResultUtil.success();
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    /**
     * 获取指定油号名称 “，”分割
     *
     * @param ableOilNumberIds 油号ids
     * @return
     */
    public String getAbleOilNumbers(String ableOilNumberIds) {
        String ableOilNumbers = "";
        if (ableOilNumberIds.contains(",")) {
            String[] strIds = ableOilNumberIds.split(",");
            List<String> strList = new ArrayList<>();
            for (String s : strIds) {
                OilNumber oilNumber = oilNumberService.selectByPrimaryKey(Integer.parseInt(s));
                strList.add(oilNumber.getOilNumber());
            }
            ableOilNumbers = String.join(",", strList);
        } else {
            OilNumber oilNumber = oilNumberService.selectByPrimaryKey(Integer.parseInt(ableOilNumberIds));
            ableOilNumbers = oilNumber.getOilNumber();
        }
        return ableOilNumbers;
    }


    /**
     * 获取指定油号列表
     *
     * @param ableOilNumberIds 油号ids
     * @return
     */
    public List<OilNumber> getOilNumberList(String ableOilNumberIds) {
        List<OilNumber> oilNumberList = new ArrayList<>();
        if (ableOilNumberIds.contains(",")) {
            String[] str = ableOilNumberIds.split(",");
            for (String s : str) {
                OilNumber oilNumber = oilNumberService.selectByPrimaryKey(Integer.parseInt(s));
                oilNumberList.add(oilNumber);
            }
        } else {
            OilNumber oilNumber = oilNumberService.selectByPrimaryKey(Integer.parseInt(ableOilNumberIds));
            oilNumberList.add(oilNumber);
        }
        return oilNumberList;
    }


    /**
     * 获取指定门店列表
     *
     * @param ableStoreIds 门店ids
     * @return
     */
    public List<Store> getStoreList(String ableStoreIds) {
        List<Store> storeList = new ArrayList<>();
        if (ableStoreIds.contains(",")) {
            String[] str = ableStoreIds.split(",");
            for (String s : str) {
                Store store = storeService.selectByPrimaryKey(Integer.parseInt(s));
                storeList.add(store);
            }
        } else {
            Store store = storeService.selectByPrimaryKey(Integer.parseInt(ableStoreIds));
            storeList.add(store);
        }
        return storeList;
    }

    /**
     * 获取优惠内容显示
     * @param list
     * @return
     */
    public List<Discount> getDiscountContent(List<Discount> list) {
        for (Discount m : list) {
            if (m.getContent() != null && !m.getContent().equals("")) {
                List<Map<String,Object>> contentArr = (List<Map<String, Object>>) JSON.parse(m.getContent());
                StringBuilder stringBuilder = new StringBuilder();

                if (m.getType().equals("0")) {
                    for (Map<String, Object> c : contentArr) {
                        stringBuilder.append("加满" + c.get("full") + "升每升减" + c.get("num") + "元 ;");
                    }
                } else if (m.getType().equals("1")) {
                    for (Map<String, Object> c : contentArr) {
                        stringBuilder.append("每满" + c.get("full") + "元减" + c.get("num") + "元 ;");
                    }
                } else if (m.getType().equals("2")) {
                    for (Map<String, Object> c : contentArr) {
                        stringBuilder.append("满" + c.get("full") + "元每升减" + c.get("num") + "元 ;");
                    }
                } else if (m.getType().equals("3")) {
                    for (Map<String, Object> c : contentArr) {
                        stringBuilder.append("充值每满" + c.get("full") + "元赠" + c.get("num") + "元 ;");
                    }
                }
                String disStr = "";
                if (stringBuilder.length() > 0) {
                    disStr = stringBuilder.toString();
                    if (";".equals(disStr.substring(disStr.length() - 1))) {
                        disStr = disStr.substring(0, disStr.length() - 1);
                    }
                }
                m.setContent(disStr);
            }
        }
        return list;
    }


}

package com.px.mgr.controller.mch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.px.db.custom.service.OrderReturnListService;
import com.px.db.model.bean.*;
import com.px.db.model.service.*;
import com.px.db.util.PageModel;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.util.DateUtil;
import com.px.util.StringUtil;
import com.px.util.mathematical.BigDecimalUtil;
import com.px.web.controller.BaseController;
import com.px.web.service.OrderReturnPayHelpService;
import com.px.web.util.Nav.NavEnum;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.login.Authorization;
import com.px.web.util.login.PermissionEnum;
import com.px.web.util.login.PermissionGroupEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 平台所有入驻商退费订单
 */
@Slf4j
@RequestMapping("/orderReturn")
@Controller
public class MchOrderReturnController extends BaseController {

    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private OrderItemService orderItemService;
    @Resource
    private OrderReturnService orderReturnService;
    @Resource
    private OrderReturnItemService orderReturnItemService;
    @Resource
    private StoreService storeService;
    @Resource
    private MchService mchService;
    @Resource
    private OrderReturnListService orderReturnListService;
    @Resource
    private OrderReturnPayHelpService orderReturnPayHelpService;


    @Authorization(PermissionEnum.mchOrderReturnList)
    @RequestMapping("/mch.html")
    public String mchInput(ModelMap map) {
        List<NavEnum> navEnumList = NavEnum.getBtnByGroup(PermissionGroupEnum.mch.name());
        List<NavEnum> navEnums = new ArrayList<>();
        for (NavEnum nav : navEnumList) {
            if (nav.getKey().equals("mchOrderReturnExport")) {
                navEnums.add(nav);
            }
        }
        //公共按钮
        map.put("btnList", navEnums);
        //列表操作选项
        map.put("actions", NavEnum.getActionByGroup(PermissionGroupEnum.mch.name()));
        //所有入驻商下拉选
        List<Mch> mchList = mchService.selectAll();
        map.put("mchList", mchList);
        //获取所有入驻商门店下拉选
        QueryHelper.SimpleQueryHandler handler = QueryHelper.startSimpleQuery();
        handler.addQuery(new QueryHelper.OrQueryModel(Store.MCH_ID_PROPERTY, 0, QueryModel.NOT_EQUALS));
        List<Store> storeList = storeService.selectByQuery(handler);
        for (Store s : storeList) {
            s.setTitle(s.getTitle() + "（" + s.getMchName() + "）");
        }
        map.put("storeList", storeList);
        return "mch/mch_order_return_list";
    }


    @Authorization(PermissionEnum.mchOrderReturnList)
    @ResponseBody
    @RequestMapping("/mchList")
    public PageModel mchlist(String orderReturnNo, Integer mchId, Integer storeId, Integer type, Integer status, Integer orderFrom, Date beginTime, Date endTime, Integer page, Integer limit) {
        //退款时间
        String beginTimeStr = "";
        if (null != beginTime) {
            beginTimeStr = DateUtil.FORMART.date2String(beginTime, "yyyy-MM-dd") + " 00:00:00";
        }
        String endTimeStr = "";
        if (null != endTime) {
            endTimeStr = DateUtil.FORMART.date2String(endTime, "yyyy-MM-dd") + " 23:59:59";
        }

        List<Map<String, Object>> list = orderReturnListService.adminOrderList(orderReturnNo, mchId, storeId, type, status, orderFrom, beginTimeStr, endTimeStr, page, limit);
        Integer count = orderReturnListService.adminOrderCount(orderReturnNo, mchId, storeId, type, status, orderFrom, beginTimeStr, endTimeStr);
        PageModel pageModel = new PageModel();
        pageModel.setList(list).setCount(count).setRel(true);
        return pageModel;
    }

    /**
     * 退费订单详情页面
     *
     * @param map
     * @param id
     * @return
     */
    @RequestMapping("/mchView.html")
    public String viewInput(ModelMap map, Integer id) {
        Map<String,Object> item = new HashMap<>();
        if (null != id) {
            item = orderReturnListService.selectOrderReturnById(id);
        }
        map.put("item", item);
        return "mch/mch_order_return_detail";
    }

    @Authorization(PermissionEnum.mchOrderReturnAudit)
    @RequestMapping("/mchAudit.html")
    public String auditInput(ModelMap map, Integer id) {
        Map<String, Object> item = new HashMap<>();
        if (null != id) {
            item = orderReturnListService.selectOrderReturnById(id);
        }
        map.put("item", item);
        return "mch/mch_order_return_audit";
    }


    /**
     * 退款审核
     *
     * @param id           退款订单id
     * @param status       是否同意退款
     * @param returnAmount 退款金额
     * @param remark       审核备注
     * @return
     */
    @ResponseBody
    @Authorization(PermissionEnum.orderReturnAudit)
    @RequestMapping("/mchAudit")
    public ResponseResultUtil audit(Integer id, Integer status, BigDecimal returnAmount, String remark) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("操作失败");
        }
        if (null == status) {
            return ResponseResultUtil.lose("操作失败，请选择审核结果");
        }
        if (null == returnAmount) {
            return ResponseResultUtil.lose("操作失败，退款金额不能为空");
        }
        if (null == remark || remark.equals("")) {
            return ResponseResultUtil.lose("操作失败，审核备注不能为空");
        }
        //判断数据是否存在
        OrderReturn oldOrderReturn = orderReturnService.selectByPrimaryKey(id);
        if (null == oldOrderReturn) {
            return ResponseResultUtil.lose("操作失败，退费订单不存在或已被删除");
        }
        OrderReturnItem oldOrderReturnItem = orderReturnItemService.selectOneEqualsT(new OrderReturnItem().setOrderReturnId(id));
        if (null == oldOrderReturnItem) {
            return ResponseResultUtil.lose("操作失败，退费订单明细不存在或已被删除");
        }
        OrderInfo oldOrderInfo = orderInfoService.selectByPrimaryKey(oldOrderReturn.getOrderId());
        if (null == oldOrderInfo) {
            return ResponseResultUtil.lose("操作失败，订单不存在或已被删除");
        }
        OrderItem oldOrderItem = orderItemService.selectOneEqualsT(new OrderItem().setOrderId(oldOrderInfo.getId().longValue()));
        if (null == oldOrderItem) {
            return ResponseResultUtil.lose("操作失败，订单明细不存在或已被删除");
        }
        //验证数据
        if (oldOrderInfo.getIsReturn()) {
            return ResponseResultUtil.lose("操作失败，订单已退款");
        }
        if (oldOrderReturn.getIsCancel()) {
            return ResponseResultUtil.lose("操作失败，退款订单已取消");
        } else if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 1)) {
            return ResponseResultUtil.lose("操作失败，退款订单为待退款状态");
        } else if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 2)) {
            return ResponseResultUtil.lose("操作失败，退款订单为退款完成状态");
        } else if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 3)) {
            return ResponseResultUtil.lose("操作失败，退款订单为拒绝退款状态");
        }

        if (returnAmount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseResultUtil.lose("退款金额必须大于等于0");
        }
        if (returnAmount.compareTo(oldOrderInfo.getOrderAmount()) > 0) {
            return ResponseResultUtil.lose("退款金额不能大于订单金额");
        }
        /**-------------------------------------------------处理退费订单数据----------------------------------------------**/
        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setId(id);
        //申请退款金额，余额退款，支付退款
        if (oldOrderReturn.getReturnAmount() != returnAmount) {
            BigDecimal refundAmount = BigDecimal.ZERO;
            BigDecimal balanceReturn = BigDecimal.ZERO;
            if (returnAmount.compareTo(oldOrderInfo.getPaidAmount()) > 0) {
                //退款金额>实际支付金额
                refundAmount = oldOrderInfo.getPaidAmount();
                balanceReturn = returnAmount.subtract(refundAmount);
            } else {
                refundAmount = returnAmount;
            }
            orderReturn.setReturnAmount(returnAmount);
            orderReturn.setBalanceReturn(balanceReturn);
            orderReturn.setRefundAmount(refundAmount);
        }
        //审核意见
        orderReturn.setAuditRemark(remark);
        //审核时间
        orderReturn.setAuditTime(new Date());
        /**-------------------------------------------------处理退费订单数据结束----------------------------------------------**/


        /**-------------------------------------------------处理退费订单明细数据----------------------------------------------**/
        OrderReturnItem orderReturnItem = new OrderReturnItem();
        orderReturnItem.setId(oldOrderReturnItem.getId());
        //退款金额
        orderReturnItem.setReturnAmount(orderReturn.getReturnAmount());
        //审核时间
        orderReturnItem.setAuditTime(orderReturn.getAuditTime());
        /**-------------------------------------------------处理退费订单明细数据结束----------------------------------------------**/

        if (status == 1) {
            //订单状态：待退款
            orderReturn.setStatus((byte) 1);
            orderReturnItem.setStatus((byte) 1);
        } else if (status == 2) {
            //订单状态：拒绝退款
            orderReturn.setStatus((byte) 3);
            orderReturnItem.setStatus((byte) 3);

        }
        //修改退费订单
        boolean updateFlag = orderReturnService.updateByPrimaryKey(orderReturn);
        if (updateFlag) {
            String[] columnName = {OrderReturn.RETURN_AMOUNT_PROPERTY, OrderReturn.REFUND_AMOUNT_PROPERTY, OrderReturn.BALANCE_RETURN_PROPERTY, OrderReturn.STATUS_PROPERTY, OrderReturn.AUDIT_REMARK_PROPERTY, OrderReturn.AUDIT_TIME_PROPERTY};
            Object[] oldValues = {oldOrderReturn.getReturnAmount(), oldOrderReturn.getBalanceReturn(), oldOrderReturn.getRefundAmount(), oldOrderReturn.getStatus(), oldOrderReturn.getAuditRemark(), oldOrderReturn.getAuditTime()};
            Object[] newValues = {orderReturn.getReturnAmount(), orderReturn.getBalanceReturn(), orderReturn.getRefundAmount(), orderReturn.getStatus(), orderReturn.getAuditRemark(), orderReturn.getAuditTime()};
            insertFiledsModifyLog(OrderReturn.TABLE_NAME, orderReturn.getId(), oldOrderReturn.getOrderReturnNo(), columnName, oldValues, newValues);
            //修改退费订单明细
            boolean updateItemFlag = orderReturnItemService.updateByPrimaryKey(orderReturnItem);
            if (updateItemFlag) {
                String[] itemColumnName = {OrderReturnItem.RETURN_AMOUNT_PROPERTY, OrderReturnItem.STATUS_PROPERTY, OrderReturnItem.AUDIT_TIME_PROPERTY};
                Object[] itemOldValues = {oldOrderReturnItem.getReturnAmount(), oldOrderReturnItem.getStatus(), oldOrderReturnItem.getAuditTime()};
                Object[] itemNewValues = {orderReturnItem.getReturnAmount(), orderReturnItem.getStatus(), orderReturnItem.getAuditTime()};
                insertFiledsModifyLog(OrderReturnItem.TABLE_NAME, orderReturnItem.getId(), "退费订单明细id：" + oldOrderReturnItem.getId(), itemColumnName, itemOldValues, itemNewValues);
                return ResponseResultUtil.success();
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    @Authorization(PermissionEnum.orderReturnAgree)
    @RequestMapping("/mchAgree.html")
    public String agreeReturnInput(ModelMap map, Integer id) {
        Map<String, Object> item = new HashMap<>();
        if (null != id) {
            item = orderReturnListService.selectOrderReturnById(id);
            OrderReturn orderReturn=orderReturnService.selectByPrimaryKey(id);
            OrderInfo orderInfo=orderInfoService.selectOneEqualsT(new OrderInfo().setOrderNo(orderReturn.getOrderNo()));
            if(orderInfo.getIntegralGiveAmount().compareTo(BigDecimal.ZERO)>0){
                map.put("giveAmount",orderInfo.getIntegralGiveAmount());
            }
        }
        map.put("item", item);
        return "mch/mch_order_return_agree";
    }

    /**
     * 退款确认
     *
     * @param id
     * @return
     */
    @ResponseBody
    @Authorization(PermissionEnum.orderReturnAgree)
    @RequestMapping("/mchAgree")
    public ResponseResultUtil agreeReturn(Integer id,Integer type) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("操作失败");
        }
        if (null == type) {
            return ResponseResultUtil.lose("操作失败");
        }
        //判断数据是否存在
        OrderReturn oldOrderReturn = orderReturnService.selectByPrimaryKey(id);
        if (null == oldOrderReturn) {
            return ResponseResultUtil.lose("操作失败，退费订单不存在或已被删除");
        }
        OrderReturnItem oldOrderReturnItem = orderReturnItemService.selectOneEqualsT(new OrderReturnItem().setOrderReturnId(id));
        if (null == oldOrderReturnItem) {
            return ResponseResultUtil.lose("操作失败，退费订单明细不存在或已被删除");
        }
        OrderInfo oldOrderInfo = orderInfoService.selectByPrimaryKey(oldOrderReturn.getOrderId());
        if (null == oldOrderInfo) {
            return ResponseResultUtil.lose("操作失败，订单不存在或已被删除");
        }
        OrderItem oldOrderItem = orderItemService.selectOneEqualsT(new OrderItem().setOrderId(oldOrderInfo.getId().longValue()));
        if (null == oldOrderItem) {
            return ResponseResultUtil.lose("操作失败，订单明细不存在或已被删除");
        }
        //验证数据
        if (oldOrderInfo.getIsReturn()) {
            return ResponseResultUtil.lose("操作失败，订单已退款");
        }
        if (oldOrderReturn.getIsCancel()) {
            return ResponseResultUtil.lose("操作失败，退款订单已取消");
        } else if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 0)) {
            return ResponseResultUtil.lose("操作失败，退款订单为待审核状态");
        } else if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 2)) {
            return ResponseResultUtil.lose("操作失败，退款订单为退款完成状态");
        }

        /**-------------------------------------------------拒绝退款----------------------------------------------**/
        if (type == 2) {
            //修改退费订单状态
            boolean updateFlag = orderReturnService.updateByPrimaryKey(new OrderReturn().setId(id).setStatus((byte) 3));
            if (updateFlag) {
                insertOneFieldModifyLog(OrderReturn.TABLE_NAME, OrderReturn.STATUS_PROPERTY, id, oldOrderReturn.getOrderReturnNo(), oldOrderReturn.getStatus(), 3);
                //修改退费订单明细状态
                boolean flag = orderReturnItemService.updateByPrimaryKey(new OrderReturnItem().setId(oldOrderReturnItem.getId()).setStatus((byte) 3));
                insertOneFieldModifyLog(OrderReturnItem.TABLE_NAME, OrderReturnItem.STATUS_PROPERTY, id, "退款订单id：" + oldOrderReturnItem.getOrderReturnId(), oldOrderReturnItem.getStatus(), 3);
                if (flag) {
                    return ResponseResultUtil.success();
                }
            }
        }
        /**-------------------------------------------------同意退款----------------------------------------------**/
        else if (type == 1) {
            if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 3)) {
                return ResponseResultUtil.lose("操作失败，退款订单为拒绝退款状态");
            }
            //退款
            return orderReturnPayHelpService.unifyOrderReturn(id, Integer.valueOf(oldOrderInfo.getPayType()));
        }
        return ResponseResultUtil.lose("操作失败");
    }

    /**
     * 退费订单删除
     *
     * @param id
     * @return
     */
    @Authorization(PermissionEnum.mchOrderReturnDel)
    @ResponseBody
    @RequestMapping("/mchDel")
    public ResponseResultUtil del(Integer id) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("操作失败");
        }
        //判断数据是否存在
        OrderReturn oldOrderReturn = orderReturnService.selectByPrimaryKey(id);
        if (null == oldOrderReturn) {
            return ResponseResultUtil.lose("操作失败，退费订单不存在或已被删除");
        }
        OrderReturnItem oldOrderReturnItem = orderReturnItemService.selectOneEqualsT(new OrderReturnItem().setOrderReturnId(id));
        if (null == oldOrderReturnItem) {
            return ResponseResultUtil.lose("操作失败，退费订单明细不存在或已被删除");
        }
        OrderInfo oldOrderInfo = orderInfoService.selectByPrimaryKey(oldOrderReturn.getOrderId());
        if (null == oldOrderInfo) {
            return ResponseResultUtil.lose("操作失败，订单不存在或已被删除");
        }
        OrderItem oldOrderItem = orderItemService.selectOneEqualsT(new OrderItem().setOrderId(oldOrderInfo.getId().longValue()));
        if (null == oldOrderItem) {
            return ResponseResultUtil.lose("操作失败，订单明细不存在或已被删除");
        }
        //判断退费订单状态
        if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 0)) {
            return ResponseResultUtil.lose("操作失败，退款订单为待审核状态");
        } else if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 1)) {
            return ResponseResultUtil.lose("操作失败，退款订单为待退款状态");
        } else if ((oldOrderReturn.getIsCancel() == false) && (oldOrderReturn.getStatus() == 3)) {
            return ResponseResultUtil.lose("操作失败，退款订单为拒绝退款状态");
        }
        //删除退费订单
        boolean delFlag = orderReturnService.deleteByPrimaryKey(id);
        if (delFlag) {
            //删除退费订单明细
            boolean delItemFlag = orderReturnItemService.deleteByPrimaryKey(oldOrderReturnItem.getId());
            if (delItemFlag) {
                //插入日志
                insertDelLog(new OrderReturn(), "退款单号" + oldOrderReturn.getOrderReturnNo(), id);
                insertDelLog(new OrderReturnItem(), "退款订单明细id：" + oldOrderReturnItem.getOrderReturnId(), id);
                return ResponseResultUtil.success();
            } else {
                orderReturnService.insertOne(oldOrderReturn);
                return ResponseResultUtil.lose("操作失败");
            }
        }
        return ResponseResultUtil.lose("操作失败");
    }


    @Authorization(PermissionEnum.mchOrderReturnExport)
    @RequestMapping("/mchExport")
    public ResponseResultUtil export(String data, HttpServletResponse response) throws IOException {
        /**--------------------------------------------------------处理数据-----------------------------------------------------------------**/
        JSONObject jsonObject = JSON.parseObject(data);
        String orderReturnNo = null;
        if (jsonObject.get("orderReturnNo") != null && !jsonObject.get("orderReturnNo").equals("")) {
            orderReturnNo = jsonObject.get("orderReturnNo").toString();
        }
        Integer mchId = null;
        if (jsonObject.get("mchId") != null && !jsonObject.get("mchId").equals("")) {
            mchId = Integer.parseInt(jsonObject.get("mchId").toString());
        }
        Integer storeId = null;
        if (jsonObject.get("storeId") != null && !jsonObject.get("storeId").equals("")) {
            storeId = Integer.parseInt(jsonObject.get("storeId").toString());
        }
        Integer type = null;
        if (jsonObject.get("type") != null && !jsonObject.get("type").equals("")) {
            type = Integer.parseInt(jsonObject.get("type").toString());
        }
        Integer status = null;
        if (jsonObject.get("status") != null && !jsonObject.get("status").equals("")) {
            status = Integer.parseInt(jsonObject.get("status").toString());
        }
        String beginTime = "";
        if (jsonObject.get("beginTime") != null && !jsonObject.get("beginTime").equals("")) {
            beginTime = DateUtil.FORMART.date2String((jsonObject.getDate("beginTime")), "yyyy-MM-dd" + " 00:00:00");
        }
        String endTime = "";
        if (jsonObject.get("endTime") != null && !jsonObject.get("endTime").equals("")) {
            endTime = DateUtil.FORMART.date2String((jsonObject.getDate("endTime")), "yyyy-MM-dd" + " 23:59:59");
        }
        List<Map<String, Object>> orderList = orderReturnListService.adminOrderList(orderReturnNo, mchId, storeId, type, status, null, beginTime, endTime, null, null);

        List<String> exportList = new ArrayList<>();
        //创建excel文档对象
        HSSFWorkbook wb = new HSSFWorkbook();
        //建立sheet对象
        HSSFSheet sheet = wb.createSheet("退费订单列表");
        //在sheet中创建第一行
        HSSFRow row = sheet.createRow(0);
        //创建单元格并设置单元格内容
        int index = 0;
        row.createCell(index++).setCellValue("退款单号");
        row.createCell(index++).setCellValue("订单单号");
        row.createCell(index++).setCellValue("昵称");
        row.createCell(index++).setCellValue("手机");
        row.createCell(index++).setCellValue("订单类型");
        row.createCell(index++).setCellValue("加油信息");
        row.createCell(index++).setCellValue("充值信息");
        row.createCell(index++).setCellValue("订单状态");
        row.createCell(index++).setCellValue("申请退款金额");
        row.createCell(index++).setCellValue("余额退款");
        row.createCell(index++).setCellValue("支付退款");
        row.createCell(index++).setCellValue("入驻商");
        row.createCell(index++).setCellValue("门店");

        row.createCell(index++).setCellValue("退款原因");
        row.createCell(index++).setCellValue("审核意见");

        row.createCell(index++).setCellValue("申请时间");
        row.createCell(index++).setCellValue("审核时间");
        row.createCell(index++).setCellValue("退款时间");
        row.createCell(index++).setCellValue("订单来源");

        for (int i = 0; i < orderList.size(); i++) {
            index = 0;
            //在sheet中创建第二行
            HSSFRow row1 = sheet.createRow(i + 1);
            //退款单号
            String orderReturnNoStr = "";
            if (null!=orderList.get(i).get("orderReturnNo")&&!"".equals(orderList.get(i).get("orderReturnNo"))){
                orderReturnNoStr = orderList.get(i).get("orderReturnNo").toString();
            }
            row1.createCell(index++).setCellValue(orderReturnNoStr);
            //订单单号
            String orderNoStr = "";
            if (null!=orderList.get(i).get("orderNo")&&!"".equals(orderList.get(i).get("orderNo"))){
                orderNoStr = orderList.get(i).get("orderNo").toString();
            }
            row1.createCell(index++).setCellValue(orderNoStr);
            //昵称
            String userNameStr = "";
            if (null!=orderList.get(i).get("userName")&&!"".equals(orderList.get(i).get("userName"))){
                userNameStr = orderList.get(i).get("userName").toString();
            }
            row1.createCell(index++).setCellValue(userNameStr);
            //手机
            String mobileStr = "";
            if (null!=orderList.get(i).get("mobile")&&!"".equals(orderList.get(i).get("mobile"))){
                mobileStr = orderList.get(i).get("mobile").toString();
            }
            row1.createCell(index++).setCellValue(mobileStr);
            //订单类型
            String typeStr = "";
            Integer typeInt = 0;
            if (null != orderList.get(i).get("type")) {
                typeInt = Integer.parseInt(orderList.get(i).get("type").toString());
                if (typeInt == 1) {
                    typeStr = "充值订单";
                } else {
                    typeStr = "加油订单";
                }
            }
            row1.createCell(index++).setCellValue(typeStr);
            //加油信息,充值信息
            String gasStr = "";
            if(typeInt==2){
                StringBuilder sb = new StringBuilder();
                if (null != orderList.get(i).get("oilNumber") && !"".equals(orderList.get(i).get("oilNumber"))) {
                    sb.append("油号：" + orderList.get(i).get("oilNumber").toString() + ",");
                }
                if (null != orderList.get(i).get("price") && !"".equals(orderList.get(i).get("price"))) {
                    sb.append("（" + orderList.get(i).get("price").toString() + "元/升）" + ",");
                }
                if (null != orderList.get(i).get("oilGun") && !"".equals(orderList.get(i).get("oilGun"))) {
                    sb.append("油枪号：" + orderList.get(i).get("oilGun").toString() + ",");
                }
                gasStr = sb.toString().substring(0, sb.toString().length() - 1);
                row1.createCell(index++).setCellValue(gasStr);
                row1.createCell(index++).setCellValue("");
            }else{
                if (null != orderList.get(i).get("cardNo") && !"".equals(orderList.get(i).get("cardNo"))) {
                    gasStr = "油卡编号："+orderList.get(i).get("cardNo").toString();
                }
                row1.createCell(index++).setCellValue("");
                row1.createCell(index++).setCellValue(gasStr);
            }
            //订单状态
            String statusStr = "";
            boolean isCancel = false;
            Integer statusInt = 0;
            if (null != orderList.get(i).get("isCancel")) {
                isCancel = (boolean) orderList.get(i).get("isCancel");
                if (isCancel) {
                    statusStr = "已取消";
                } else {
                    if (null != orderList.get(i).get("status")) {
                        statusInt = Integer.parseInt(orderList.get(i).get("status").toString());
                        if (statusInt == 0) {
                            statusStr = "待审核";
                        } else if (statusInt == 1) {
                            statusStr = "待退款";
                        } else if (statusInt == 2) {
                            statusStr = "退款完成";
                        } else if (statusInt == 3) {
                            statusStr = "拒绝退款";
                        }
                    }
                }
            }
            row1.createCell(index++).setCellValue(statusStr);

            //申请退款金额
            String applyReturnAmountStr = "";
            if (null != orderList.get(i).get("applyReturnAmount")) {
                BigDecimal applyReturnAmountBig = new BigDecimal(orderList.get(i).get("applyReturnAmount").toString());
                applyReturnAmountStr = BigDecimalUtil.round(applyReturnAmountBig, 2, 2).toString();
            }
            row1.createCell(index++).setCellValue(applyReturnAmountStr);
            //余额退款
            String balanceReturnStr = "";
            BigDecimal balanceReturnBig = BigDecimal.ZERO;
            if (null != orderList.get(i).get("balanceReturn")) {
                balanceReturnBig = new BigDecimal(orderList.get(i).get("balanceReturn").toString());
                balanceReturnStr = BigDecimalUtil.round(balanceReturnBig, 2, 2).toString();
            }
            row1.createCell(index++).setCellValue(balanceReturnStr);
            //支付退款
            String refundAmountStr = "";
            BigDecimal refundAmountBig = BigDecimal.ZERO;
            if (null != orderList.get(i).get("refundAmount")) {
                refundAmountBig = new BigDecimal(orderList.get(i).get("refundAmount").toString());
                refundAmountStr = BigDecimalUtil.round(refundAmountBig, 2, 2).toString();
            }
            row1.createCell(index++).setCellValue(refundAmountStr);
            //入驻商
            String mchNameStr = "";
            if (null!=orderList.get(i).get("mchName")&&!"".equals(orderList.get(i).get("mchName"))){
                mchNameStr = orderList.get(i).get("mchName").toString();
            }
            row1.createCell(index++).setCellValue(mchNameStr);
            //门店
            String storeNameStr = "";
            if (null!=orderList.get(i).get("storeName")&&!"".equals(orderList.get(i).get("storeName"))){
                storeNameStr = orderList.get(i).get("storeName").toString();
            }
            row1.createCell(index++).setCellValue(storeNameStr);


            //退款原因
            String remarkStr = "";
            if (null != orderList.get(i).get("remark") && !"".equals(orderList.get(i).get("remark"))) {
                remarkStr = orderList.get(i).get("remark").toString();
            }
            row1.createCell(index++).setCellValue(remarkStr);
            //审核意见
            String auditRemarkStr = "";
            if (null != orderList.get(i).get("auditRemark") && !"".equals(orderList.get(i).get("auditRemark"))) {
                auditRemarkStr = orderList.get(i).get("auditRemark").toString();
            }
            row1.createCell(index++).setCellValue(auditRemarkStr);


            //下单时间
            String createTimeStr = "";
            if (null != orderList.get(i).get("createTime")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                createTimeStr = sdf.format(orderList.get(i).get("createTime"));
            }
            row1.createCell(index++).setCellValue(createTimeStr);
            //审核时间
            String auditTimeStr = "";
            if (null != orderList.get(i).get("auditTime")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                auditTimeStr = sdf.format(orderList.get(i).get("auditTime"));
            }
            row1.createCell(index++).setCellValue(auditTimeStr);
            //退款时间
            String refundTimeStr = "";
            if (null != orderList.get(i).get("refundTime")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                refundTimeStr = sdf.format(orderList.get(i).get("refundTime"));
            }
            row1.createCell(index++).setCellValue(refundTimeStr);
            //来源
            String orderFromStr = "";
            if (null != orderList.get(i).get("orderFrom")) {
                Integer orderFromInt = Integer.parseInt(orderList.get(i).get("orderFrom").toString());
                if (orderFromInt==0){
                    orderFromStr = "小程序";
                }else if (orderFromInt == 1){
                    orderFromStr = "APP";
                }else if (orderFromInt==2){
                    orderFromStr = "后台补单";
                }
            }
            row1.createCell(index++).setCellValue(orderFromStr);
            exportList.add(orderList.get(i).toString());
        }
        //输出excel文件
        try {
            OutputStream os = response.getOutputStream();
            response.reset();
            response.setHeader("Content-disposition", "attachment;filename=" + toUtf8String(StringUtil.nullToBlank("退费订单列表.xls")));
            response.setContentType("application/msexcel");
            wb.write(os);
            os.close();
            insertExportLog(new OrderInfo(), exportList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

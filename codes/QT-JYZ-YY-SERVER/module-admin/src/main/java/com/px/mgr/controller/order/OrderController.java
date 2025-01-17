package com.px.mgr.controller.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.px.db.custom.service.OrderHelpService;
import com.px.db.model.bean.*;
import com.px.db.model.service.OrderInfoService;
import com.px.db.model.service.OrderSplitService;
import com.px.db.model.service.StoreService;
import com.px.db.model.service.UserService;
import com.px.db.util.PageModel;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.db.util.SqlSortModel;
import com.px.util.DateUtil;
import com.px.util.StringUtil;
import com.px.util.mathematical.BigDecimalUtil;
import com.px.web.controller.BaseController;
import com.px.web.service.CommonHelpService;
import com.px.web.util.Nav.NavEnum;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.lakala.LakalaApiService;
import com.px.web.util.login.Authorization;
import com.px.web.util.login.LoginUtil;
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
 * 平台自营订单
 */
@Slf4j
@RequestMapping("/order")
@Controller
public class OrderController extends BaseController {

    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private OrderHelpService orderHelpService;
    @Resource
    private StoreService storeService;
    @Resource
    private CommonHelpService commonHelpService;
    @Resource
    private UserService userService;
    @Resource
    private LakalaApiService lakalaApiService;
    @Resource
    private OrderSplitService orderSplitService;
    /**
     * 平台自营订单页面
     *
     * @param map
     * @return
     */
    @Authorization(PermissionEnum.orderList)
    @RequestMapping("/list.html")
    public String listInput(ModelMap map) {
        List<NavEnum> navEnumList = NavEnum.getBtnByGroup(PermissionGroupEnum.order.name());
        List<NavEnum> navEnums = new ArrayList<>();
        for (NavEnum nav : navEnumList) {
            if (nav.getKey().equals("orderExport")) {
                navEnums.add(nav);
            }
        }
        //公共按钮
        map.put("btnList", navEnums);
        //列表操作选项
        map.put("actions", NavEnum.getActionByGroup(PermissionGroupEnum.order.name()));
        //平台自营的所有门店
        List<Store> storeList = storeService.selectEqualsT(new Store().setMchId(0));
        map.put("storeList", storeList);
        return "order/list";
    }


    @Authorization(PermissionEnum.orderList)
    @ResponseBody
    @RequestMapping("/list")
    public PageModel list(String orderNo, String verifyNo, Integer storeId, Integer type,
                          Integer payStatus, Integer verifyStatus, Integer printTicket,
                          Integer orderFrom, Date beginTime, Date endTime,
                          Integer page, Integer limit) {
        //下单时间
        String beginTimeStr = "";
        if (null != beginTime) {
            beginTimeStr = DateUtil.FORMART.date2String(beginTime, "yyyy-MM-dd") + " 00:00:00";
        }
        String endTimeStr = "";
        if (null != endTime) {
            endTimeStr = DateUtil.FORMART.date2String(endTime, "yyyy-MM-dd") + " 23:59:59";
        }

        List<Map<String, Object>> list = orderHelpService.mchOrderList(orderNo, verifyNo, storeId, type,
                payStatus, verifyStatus, printTicket,
                orderFrom, beginTimeStr, endTimeStr,
                0, null, page, limit);

        Integer count = orderHelpService.mchOrderCount(orderNo, verifyNo, storeId, type,
                payStatus, verifyStatus, printTicket,
                orderFrom, beginTimeStr, endTimeStr,
                0, null);

        PageModel pageModel = new PageModel();
        pageModel.setList(list).setCount(count).setRel(true);
        return pageModel;
    }


    /**
     * 小票打印
     *
     * @param id 订单id
     * @return
     */
    @Authorization(PermissionEnum.orderPrint)
    @ResponseBody
    @RequestMapping("/orderPrint")
    public ResponseResultUtil list(Integer id) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("小票打印失败");
        }
        //判断数据是否存在
        OrderInfo orderInfo = orderInfoService.selectByPrimaryKey(id);
        if (null == orderInfo) {
            return ResponseResultUtil.lose("小票打印失败");
        }
        //小票打印
        boolean flag = commonHelpService.orderPrint(id);
        if (flag) {
            return ResponseResultUtil.success();
        }
        return ResponseResultUtil.lose();
    }

    /**
     * 订单详情页面
     *
     * @param map
     * @param id
     * @return
     */
    @Authorization(PermissionEnum.orderList)
    @RequestMapping("/view.html")
    public String viewInput(ModelMap map, Integer id) {
        Map<String, Object> item = new HashMap<>();
        if (null != id) {
            item = orderHelpService.selectOrderById(id);
            if (item.get("discountContent") != null && !item.get("discountContent").equals("")) {
                List<Map<String, Object>> contentMap = (List<Map<String, Object>>) JSON.parse(item.get("discountContent").toString());
                if (contentMap.size() > 0) {
                    String contentStr = String.valueOf(contentMap.get(0).get("content"));
                    item.put("discountContent", contentStr);
                }
            }
        }
        map.put("item", item);
        return "order/detail";
    }

    @Authorization(PermissionEnum.orderExport)
    @RequestMapping("/export")
    public ResponseResultUtil export(String data, HttpServletResponse response) throws IOException {
        /**--------------------------------------------------------处理数据-----------------------------------------------------------------**/
        JSONObject jsonObject = JSON.parseObject(data);
        String orderNo = null;
        if (jsonObject.get("orderNo") != null && !jsonObject.get("orderNo").equals("")) {
            orderNo = jsonObject.get("orderNo").toString();
        }
        Integer storeId = null;
        if (jsonObject.get("storeId") != null && !jsonObject.get("storeId").equals("")) {
            storeId = Integer.parseInt(jsonObject.get("storeId").toString());
        }
        Integer type = null;
        if (jsonObject.get("type") != null && !jsonObject.get("type").equals("")) {
            type = Integer.parseInt(jsonObject.get("type").toString());
        }
        Integer payStatus = null;
        if (jsonObject.get("payStatus") != null && !jsonObject.get("payStatus").equals("")) {
            payStatus = Integer.parseInt(jsonObject.get("payStatus").toString());
        }
        Integer printTicket = null;
        if (jsonObject.get("printTicket") != null && !jsonObject.get("printTicket").equals("")) {
            printTicket = Integer.parseInt(jsonObject.get("printTicket").toString());
        }
        String beginTime = "";
        if (jsonObject.get("beginTime") != null && !jsonObject.get("beginTime").equals("")) {
            beginTime = DateUtil.FORMART.date2String((jsonObject.getDate("beginTime")), "yyyy-MM-dd" + " 00:00:00");
        }
        String endTime = "";
        if (jsonObject.get("endTime") != null && !jsonObject.get("endTime").equals("")) {
            endTime = DateUtil.FORMART.date2String((jsonObject.getDate("endTime")), "yyyy-MM-dd" + " 23:59:59");
        }
        List<Map<String, Object>> orderList = orderHelpService.mchOrderList(orderNo, null, storeId, type, payStatus, null, printTicket, null, beginTime, endTime, 0, null, null, null);
        List<String> exportList = new ArrayList<>();
        //创建excel文档对象
        HSSFWorkbook wb = new HSSFWorkbook();
        //建立sheet对象
        HSSFSheet sheet = wb.createSheet("订单列表");
        //在sheet中创建第一行
        HSSFRow row = sheet.createRow(0);
        //创建单元格并设置单元格内容
        int index = 0;
        row.createCell(index++).setCellValue("订单单号");
        row.createCell(index++).setCellValue("昵称");
        row.createCell(index++).setCellValue("手机");
        row.createCell(index++).setCellValue("订单类型");
        row.createCell(index++).setCellValue("加油信息");
        row.createCell(index++).setCellValue("充值信息");
        row.createCell(index++).setCellValue("订单状态");
        row.createCell(index++).setCellValue("商品总计");
        row.createCell(index++).setCellValue("优惠金额");
        row.createCell(index++).setCellValue("赠送金额");
        row.createCell(index++).setCellValue("订单金额");
        row.createCell(index++).setCellValue("油卡抵扣");
        row.createCell(index++).setCellValue("实付金额");
        row.createCell(index++).setCellValue("入驻商");
        row.createCell(index++).setCellValue("门店");
        row.createCell(index++).setCellValue("优惠信息");
        row.createCell(index++).setCellValue("下单时间");
        row.createCell(index++).setCellValue("支付方式");
        row.createCell(index++).setCellValue("支付时间");
        row.createCell(index++).setCellValue("打印时间");
        row.createCell(index++).setCellValue("小票打印次数");
        row.createCell(index++).setCellValue("订单来源");

        for (int i = 0; i < orderList.size(); i++) {
            index = 0;
            //在sheet中创建第二行
            HSSFRow row1 = sheet.createRow(i + 1);
            //订单单号
            String orderNoStr = "";
            if (null != orderList.get(i).get("orderNo") && !"".equals(orderList.get(i).get("orderNo"))) {
                orderNoStr = orderList.get(i).get("orderNo").toString();
            }
            row1.createCell(index++).setCellValue(orderNoStr);
            //昵称
            String userNameStr = "";
            if (null != orderList.get(i).get("userName") && !"".equals(orderList.get(i).get("userName"))) {
                userNameStr = orderList.get(i).get("userName").toString();
            }
            row1.createCell(index++).setCellValue(userNameStr);
            //手机
            String mobileStr = "";
            if (null != orderList.get(i).get("mobile") && !"".equals(orderList.get(i).get("mobile"))) {
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
            if (typeInt == 2) {
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
            } else {
                if (null != orderList.get(i).get("cardNo") && !"".equals(orderList.get(i).get("cardNo"))) {
                    gasStr = "油卡编号：" + orderList.get(i).get("cardNo").toString();
                }
                row1.createCell(index++).setCellValue("");
                row1.createCell(index++).setCellValue(gasStr);
            }
            //订单状态：取消->退款->支付状态
            String payStatusStr = "";
            boolean isCancelFlag = false;
            boolean isReturnFlag = false;
            Integer payStatusInt = 0;
            if (null != orderList.get(i).get("isCancel")) {
                isCancelFlag = (boolean) orderList.get(i).get("isCancel");
                if (isCancelFlag) {
                    payStatusStr = "已取消";
                } else {
                    if (null != orderList.get(i).get("isReturn")) {
                        isReturnFlag = (boolean) orderList.get(i).get("isReturn");
                        if (isReturnFlag) {
                            payStatusStr = "已退款";
                        } else {
                            if (null != orderList.get(i).get("payStatus")) {
                                payStatusInt = Integer.parseInt(orderList.get(i).get("payStatus").toString());
                                if (payStatusInt == 0) {
                                    payStatusStr = "待支付";
                                } else if (payStatusInt == 1) {
                                    payStatusStr = "部分支付";
                                } else if (payStatusInt == 2) {
                                    payStatusStr = "支付完成";
                                }
                            }
                        }
                    }
                }
            }
            row1.createCell(index++).setCellValue(payStatusStr);
            //商品总计
            String totalAmountStr = "";
            if (null != orderList.get(i).get("totalAmount")) {
                BigDecimal totalAmountBig = new BigDecimal(orderList.get(i).get("totalAmount").toString());
                totalAmountStr = BigDecimalUtil.round(totalAmountBig, 2, 2).toString();
            }
            row1.createCell(index++).setCellValue(totalAmountStr);
            //优惠金额,赠送金额
            String discountAmountStr = "";
            if (null != orderList.get(i).get("discountAmount")) {
                BigDecimal discountAmountBig = new BigDecimal(orderList.get(i).get("discountAmount").toString());
                discountAmountStr = BigDecimalUtil.round(discountAmountBig, 2, 2).toString();
            }
            if (typeInt == 1) {
                row1.createCell(index++).setCellValue("0.00");
                row1.createCell(index++).setCellValue(discountAmountStr);
            } else {
                row1.createCell(index++).setCellValue(discountAmountStr);
                row1.createCell(index++).setCellValue("0.00");
            }
            //订单金额
            String orderAmountStr = "";
            BigDecimal orderAmountBig = BigDecimal.ZERO;
            if (null != orderList.get(i).get("orderAmount")) {
                orderAmountBig = new BigDecimal(orderList.get(i).get("orderAmount").toString());
                orderAmountStr = BigDecimalUtil.round(orderAmountBig, 2, 2).toString();
            }
            row1.createCell(index++).setCellValue(orderAmountStr);
            //油卡抵扣
            String balancePayStr = "";
            BigDecimal balancePayBig = BigDecimal.ZERO;
            if (null != orderList.get(i).get("balancePay")) {
                balancePayBig = new BigDecimal(orderList.get(i).get("balancePay").toString());
                balancePayStr = BigDecimalUtil.round(balancePayBig, 2, 2).toString();
            }
            row1.createCell(index++).setCellValue(balancePayStr);
            //实付金额
            String paidAmountStr = "";
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal paidAmountBig = BigDecimal.ZERO;
            if (null != orderList.get(i).get("paidAmount")) {
                paidAmountBig = new BigDecimal(orderList.get(i).get("paidAmount").toString());
            }

            if (isCancelFlag) {
                amount = paidAmountBig;
            } else {
                if (isReturnFlag) {
                    amount = orderAmountBig.subtract(balancePayBig);
                } else {
                    if (null != orderList.get(i).get("payStatus")) {
                        if (payStatusInt == 0) {
                            amount = paidAmountBig;
                        } else if (payStatusInt == 1) {
                            amount = orderAmountBig.subtract(balancePayBig);
                            amount = amount.subtract(paidAmountBig);
                        } else if (payStatusInt == 2) {
                            amount = orderAmountBig.subtract(balancePayBig);
                        }
                    }
                }
            }
            paidAmountStr = BigDecimalUtil.round(amount, 2, 2).toString();
            row1.createCell(index++).setCellValue(paidAmountStr);
            //入驻商
            String mchNameStr = "";
            if (null != orderList.get(i).get("mchName") && !"".equals(orderList.get(i).get("mchName"))) {
                mchNameStr = orderList.get(i).get("mchName").toString();
            }
            row1.createCell(index++).setCellValue(mchNameStr);
            //门店
            String storeNameStr = "";
            if (null != orderList.get(i).get("storeName") && !"".equals(orderList.get(i).get("storeName"))) {
                storeNameStr = orderList.get(i).get("storeName").toString();
            }
            row1.createCell(index++).setCellValue(storeNameStr);
            //优惠信息
            String discountContentStr = "";
            if (null != orderList.get(i).get("discountContent") && !"".equals(orderList.get(i).get("discountContent"))) {
                List<Map<String, Object>> mapList = (List<Map<String, Object>>) JSON.parse(orderList.get(i).get("discountContent").toString());
                if (mapList.size() > 0) {
                    discountContentStr = mapList.get(0).get("content").toString();
                }
            }
            row1.createCell(index++).setCellValue(discountContentStr);
            //下单时间
            String createTimeStr = "";
            if (null != orderList.get(i).get("createTime")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                createTimeStr = sdf.format(orderList.get(i).get("createTime"));
            }
            row1.createCell(index++).setCellValue(createTimeStr);
            row1.createCell(index++).setCellValue(StringUtil.nullToBlank(orderList.get(i).get("payTypeName")));

            //支付时间
            String payTimeStr = "";
            if (null != orderList.get(i).get("payTime")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                payTimeStr = sdf.format(orderList.get(i).get("payTime"));
            }
            row1.createCell(index++).setCellValue(payTimeStr);
            //打印时间
            String printTicketTimeStr = "";
            if (null != orderList.get(i).get("printTicketTime")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                printTicketTimeStr = sdf.format(orderList.get(i).get("printTicketTime"));
            }
            row1.createCell(index++).setCellValue(printTicketTimeStr);
            //小票打印次数
            Integer printTicketCount = 0;
            if (null != orderList.get(i).get("printTicketCount")) {
                printTicketCount = Integer.parseInt(orderList.get(i).get("printTicketCount").toString());
            }
            row1.createCell(index++).setCellValue(printTicketCount.toString());
            //来源
            String orderFromStr = "";
            if (null != orderList.get(i).get("orderFrom")) {
                Integer orderFromInt = Integer.parseInt(orderList.get(i).get("orderFrom").toString());
                if (orderFromInt == 0) {
                    orderFromStr = "小程序";
                } else if (orderFromInt == 1) {
                    orderFromStr = "APP";
                } else if (orderFromInt == 2) {
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
            response.setHeader("Content-disposition", "attachment;filename=" + toUtf8String(StringUtil.nullToBlank("订单列表.xls")));
            response.setContentType("application/msexcel");
            wb.write(os);
            os.close();
            insertExportLog(new OrderInfo(), exportList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 订单分账
     */
    @ResponseBody
    @RequestMapping("/split")
    public ResponseResultUtil split(Integer id) {

        OrderInfo o = orderInfoService.selectByPrimaryKey(id);
        lakalaApiService.separate(o);
        return ResponseResultUtil.success();
    }

    /**
     * 订单分账
     */
    @ResponseBody
    @RequestMapping("/splitQuery")
    public ResponseResultUtil splitQuery(Integer id) {
        List<OrderSplit> orderSplits = orderSplitService.selectEqualsT(new OrderSplit().setOrderId(id).setStatus(0).setType(0));
        if(orderSplits.size() == 0){
            return ResponseResultUtil.lose("没有请求中的分账");
        }
        return lakalaApiService.separateQuery(orderSplits.get(0));
    }
    /**
     * 订单分账
     */
    @ResponseBody
    @RequestMapping("/splitCancel")
    public ResponseResultUtil splitCancel(Integer id) {
        List<OrderSplit> orderSplits = orderSplitService.selectEqualsT(new OrderSplit().setOrderId(id).setStatus(1).setType(0));
        if(orderSplits.size() == 0){
            return ResponseResultUtil.lose("没有已完成的分账");
        }
        lakalaApiService.separateCancel(orderSplits.get(0));
        return ResponseResultUtil.success();
    }

    /**
     * 订单分账
     */
    @ResponseBody
    @RequestMapping("/splitFallback")
    public ResponseResultUtil splitFallback(Integer id) {
        List<OrderSplit> orderSplits = orderSplitService.selectEqualsT(new OrderSplit().setOrderId(id).setStatus(1).setType(0));
        if(orderSplits.size() == 0){
            return ResponseResultUtil.lose("没有已完成的分账");
        }
        lakalaApiService.separateFallback(orderSplits.get(0));
        return ResponseResultUtil.success();
    }
}

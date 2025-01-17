package com.px.mgr.controller.report;

import com.github.pagehelper.PageInfo;
import com.px.db.base.BaseCustomMapper;
import com.px.db.custom.bean.FlowSearchParams;
import com.px.db.custom.service.ReportService;
import com.px.db.model.bean.AdminUser;
import com.px.db.model.bean.Flow;
import com.px.db.model.bean.Store;
import com.px.db.model.service.FlowService;
import com.px.db.model.service.StoreService;
import com.px.db.util.PageModel;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.db.util.SqlSortModel;
import com.px.util.DateUtil;
import com.px.util.MyExistUtil;
import com.px.util.StringUtil;
import com.px.web.controller.BaseController;
import com.px.web.util.Nav.NavEnum;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.login.Authorization;
import com.px.web.util.login.LoginUtil;
import com.px.web.util.login.PermissionEnum;
import com.px.web.util.login.PermissionGroupEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.*;

@RequestMapping("flow")
@Controller
@Slf4j
public class FlowController extends BaseController {
    @Resource
    private FlowService flowService;
    @Resource
    private StoreService storeService;
    @Resource
    private ReportService reportService;

    @RequestMapping("/list.html")
    @Authorization(PermissionEnum.flowList)
    public String listInput(ModelMap model) {
        //操作按钮
        model.put("actions", NavEnum.getActionByGroup(PermissionGroupEnum.flow.name()));
        //公共按钮
        model.put("btnList", NavEnum.getBtnByGroup(PermissionGroupEnum.flow.name()));
        //获取入驻商id
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);
        Integer mchId = user.getMchId();
        List<Store> storeList = storeService.selectEqualsT(new Store().setMchId(mchId));
        model.put("storeList", storeList);
        return "finance/flow";
    }

    @RequestMapping("/list")
    @Authorization(PermissionEnum.flowList)
    @ResponseBody
    public PageModel list(FlowSearchParams search, Integer page, Integer limit) {
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);
        Integer mchId = user.getMchId();
        search.setMchId(mchId);
        QueryHelper.SimpleQueryHandler handler = getHandler(search);
        PageInfo<Flow> pageInfo = flowService.selectByQueryWithPage(handler, page, limit
                , new SqlSortModel(Flow.CREATE_TIME_PROPERTY, false), new SqlSortModel(Flow.PRIMARY_KEY_FIELD, false)
        );
        return new PageModel<>(pageInfo);
    }

    private QueryHelper.SimpleQueryHandler getHandler(FlowSearchParams search) {
        QueryHelper.SimpleQueryHandler handler = QueryHelper.startSimpleQuery();
        if (search.getMchId() != null) {
            handler.addQuery(Flow.MCH_ID_FIELD, search.getMchId());
        }
        if (MyExistUtil.isGreaterThan0Integer(search.getStoreId())) {
            handler.addQuery(Flow.STORE_ID_FIELD, search.getStoreId());
        }
        if (MyExistUtil.notEmptyString(search.getBeginTime())) {
            handler.addQuery(Flow.CREATE_TIME_FIELD, search.getBeginTime(), QueryModel.GREATER_EQUALS);
        }
        if (MyExistUtil.notEmptyString(search.getEndTime())) {
            handler.addQuery(Flow.CREATE_TIME_FIELD, search.getEndTime(), QueryModel.LESS_EQUALS);
        }
        if (MyExistUtil.notEmptyString(search.getPlatform())) {
            handler.addQuery(Flow.PLATFORM_FIELD, search.getPlatform());
        }
        return handler;
    }

    @RequestMapping("/export")
    @Authorization(PermissionEnum.flowList)
    public ResponseResultUtil export(FlowSearchParams search, HttpServletResponse response) throws IOException {
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);
        Integer mchId = user.getMchId();
        search.setMchId(mchId);
        QueryHelper.SimpleQueryHandler handler = getHandler(search);
        List<Flow> flowList = flowService.selectByQuery(handler,
                new SqlSortModel(Flow.CREATE_TIME_PROPERTY, false), new SqlSortModel(Flow.PRIMARY_KEY_FIELD, false)
        );
        //状态名称列表
        Map<Integer, String> statusName = new HashMap<>();
        statusName.put(0, "待结算");
        statusName.put(1, "结算中");
        statusName.put(2, "完成");
        statusName.put(3, "结算失败");
        //类型名称列表
        Map<String, String> typeName = new HashMap<>();
        typeName.put("pay", "收款");
        typeName.put("rate", "支付手续费");
        typeName.put("share", "技术服务费");
        typeName.put("refund", "退款");

        //订单类型名称列表
        Map<Integer, String> orderTypeName = new HashMap<>();
        orderTypeName.put(1, "充值订单");
        orderTypeName.put(2, "加油订单");
        orderTypeName.put(3, "积分订单");
        orderTypeName.put(9, "退款订单");

        //创建HSSFWorkbook对象(excel的文档对象)
        SXSSFWorkbook wb = new SXSSFWorkbook();
        //建立新的sheet对象（excel的表单）
        SXSSFSheet sheet = wb.createSheet("资金流水");
        //第一行：标题
        SXSSFRow row2 = sheet.createRow(0);
        int index = 0;
        row2.createCell(index++).setCellValue("交易时间");
        row2.createCell(index++).setCellValue("入驻商");

        row2.createCell(index++).setCellValue("加油站");
        row2.createCell(index++).setCellValue("交易金额");
        row2.createCell(index++).setCellValue("交易状态");
        row2.createCell(index++).setCellValue("交易类型");
        row2.createCell(index++).setCellValue("交易详情");
        row2.createCell(index++).setCellValue("结算方式");
        row2.createCell(index++).setCellValue("收款平台");
        row2.createCell(index++).setCellValue("商户");
        row2.createCell(index++).setCellValue("商户编号");
        row2.createCell(index++).setCellValue("第三方交易单号");
        row2.createCell(index++).setCellValue("费率");
        row2.createCell(index++).setCellValue("相关订单号");
        row2.createCell(index++).setCellValue("订单类型");
        int i = 1;
        for (Flow f : flowList) {
            index = 0;
            //在sheet里创建第三行
            SXSSFRow row3 = sheet.createRow(i++);
            row3.createCell(index++).setCellValue(f.getStatus() == 2 ? StringUtil.nullToBlank(DateUtil.FORMART.date2String(f.getFinishTime(), "yyyy/MM/dd HH:mm:ss")) : "");
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getMchName()));
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getStoreName()));
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getAmount()));
            row3.createCell(index++).setCellValue(statusName.containsKey(f.getStatus()) ? statusName.get(f.getStatus()) : "");
            row3.createCell(index++).setCellValue(typeName.containsKey(f.getType()) ? typeName.get(f.getType()) : "其它");
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getContent()));
            row3.createCell(index++).setCellValue(f.getSettlementType() == 0 ? "自动结算" : "人工结算");

            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getPlatformName()));
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getPayMchName()));
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getPayMchId()));
//            row3.createCell(index++).setCellValue(f.getType().equals("share")?f.getShareReceiverTypeName():"--");
//            row3.createCell(index++).setCellValue(f.getType().equals("share")?f.getShareReceiverId():"--");
//            row3.createCell(index++).setCellValue(f.getType().equals("share")?f.getShareReceiverName():"--");
            row3.createCell(index++).setCellValue(f.getType().equals("rate") ? f.getRate() + "%" : "--");

            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getPayOrderNo()));
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.getOrderNo()));
            row3.createCell(index++).setCellValue(orderTypeName.containsKey(f.getOrderType()) ? orderTypeName.get(f.getOrderType()) : "其它");
        }
        //.....省略部分代码
        //输出Excel文件
        OutputStream output = response.getOutputStream();
        response.reset();
        response.setHeader("Content-disposition", "attachment; filename="
                + StringUtil.toUtf8String("资金流水" + DateUtil.FORMART.date2String(new Date(), "yyyyMMddHHmmss") + ".xls")
        );
        response.setContentType("application/msexcel");
        wb.write(output);
        output.close();
        /**
         * 报表导出记录日志
         */
        insertReportExportLog("资金流水", new ArrayList<>());
        return null;
    }


    @RequestMapping("/total")
    @ResponseBody
    public ResponseResultUtil total(FlowSearchParams search) {
        Map<String, Object> data = new HashMap<>();
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);
        Integer mchId = user.getMchId();
        search.setMchId(mchId);
        QueryHelper.SimpleQueryHandler handler = getHandler(search);
        //统计收款、退款、支付手续费和技术服务费
        List<BaseCustomMapper.AggregateNumber> list = flowService.getSumByQueryAndGroup(handler, Flow.AMOUNT_FIELD, Flow.TYPE_PROPERTY);
        BigDecimal total = BigDecimal.ZERO;
        for (BaseCustomMapper.AggregateNumber d : list) {
            data.put(d.getGroupProperty(), d.getAggregateValue());
            total = total.add(d.getAggregateValue());
        }
        handler.addQuery(Flow.TYPE_PROPERTY, "share");
        //统计已结算和未结算分账
        List<BaseCustomMapper.AggregateNumber> list2 = flowService.getSumByQueryAndGroup(handler, Flow.AMOUNT_FIELD, Flow.STATUS_FIELD);
        BigDecimal shareProcess = BigDecimal.ZERO;
        for (BaseCustomMapper.AggregateNumber d : list2) {
            if (d.getGroupProperty().equals("2")) {
                data.put("shareFinish", d.getAggregateValue());
            } else {
                shareProcess = shareProcess.add(d.getAggregateValue()).setScale(2, 4);
            }
        }
        data.put("shareProcess", shareProcess);
        data.put("total", total);
        return ResponseResultUtil.success(data);
    }

    @RequestMapping("/report.html")
    @Authorization(PermissionEnum.flowReport)
    public String reportHtml(ModelMap model) {
        //操作按钮
        //公共按钮
        model.put("btnList", NavEnum.getBtnByGroup(PermissionGroupEnum.flowReport.name()));
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);
        Integer mchId = user.getMchId();
        List<Store> storeList = storeService.selectEqualsT(new Store().setMchId(mchId));
        model.put("storeList", storeList);
        return "finance/flow_report";
    }

    @RequestMapping("/report")
    @Authorization(PermissionEnum.flowReport)
    @ResponseBody
    public PageModel report(FlowSearchParams search, Integer page, Integer limit) {
        if (search.getGroupBy() == null) {
            search.setGroupBy("store");
        }
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);
        Integer mchId = user.getMchId();
        search.setMchId(mchId);
        PageInfo<Map<String, Object>> resultPage = new PageInfo<>();
        List<Map<String, Object>> list = reportService.getFlowReport(search, null, (page - 1) * limit, limit);
        resultPage.setList(list);
        resultPage.setTotal(reportService.getFlowReport(search, null, null, null).size());
        return new PageModel<>(resultPage);
    }

    @RequestMapping("/reportExport")
    @Authorization(PermissionEnum.flowReportExport)
    public ResponseResultUtil reportExport(FlowSearchParams search, HttpServletResponse response) throws IOException {
        if (search.getGroupBy() == null) {
            search.setGroupBy("store");
        }
        AdminUser user = LoginUtil.getUserCache(AdminUser.class);
        Integer mchId = user.getMchId();
        search.setMchId(mchId);
        List<Map<String, Object>> list = reportService.getFlowReport(search, null, null, null);

        //创建HSSFWorkbook对象(excel的文档对象)
        SXSSFWorkbook wb = new SXSSFWorkbook();
        //建立新的sheet对象（excel的表单）
        SXSSFSheet sheet = wb.createSheet("资金流水统计");
        //第一行：标题
        SXSSFRow row2 = sheet.createRow(0);
        int index = 0;
        row2.createCell(index++).setCellValue("加油站");
        if (search.getGroupBy().equals("storePlatform") || search.getGroupBy().equals("storePlatformMch")) {
            row2.createCell(index++).setCellValue("收款平台");
        }
        if (search.getGroupBy().equals("storePlatformMch")) {
            row2.createCell(index++).setCellValue("收款商户名称");
            row2.createCell(index++).setCellValue("收款商户编号");
        }
        row2.createCell(index++).setCellValue("收款金额（元）");
        row2.createCell(index++).setCellValue("退款金额（元）");
        row2.createCell(index++).setCellValue("支付手续费（元）");
        row2.createCell(index++).setCellValue("净收入（元）");

        int i = 1;
        for (Map<String, Object> f : list) {
            index = 0;
            //在sheet里创建第三行
            SXSSFRow row3 = sheet.createRow(i++);

            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.get("storeName")));
            if (search.getGroupBy().equals("storePlatform") || search.getGroupBy().equals("storePlatformMch")) {
                row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.get("platformName")));
            }
            if (search.getGroupBy().equals("storePlatformMch")) {
                row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.get("payMchName")));
                row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.get("payMchId")));
            }
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.get("totalPay")));
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.get("totalRefund")));
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.get("totalRate")));
            row3.createCell(index++).setCellValue(StringUtil.nullToBlank(f.get("netIncome")));
        }
        //.....省略部分代码
        //输出Excel文件
        OutputStream output = response.getOutputStream();
        response.reset();
        response.setHeader("Content-disposition", "attachment; filename="
                + StringUtil.toUtf8String("资金流水统计" + DateUtil.FORMART.date2String(new Date(), "yyyyMMddHHmmss") + ".xls")
        );
        response.setContentType("application/msexcel");
        wb.write(output);
        output.close();
        /**
         * 报表导出记录日志
         */
        insertReportExportLog("资金流水统计", new ArrayList<>());
        return null;
    }
}

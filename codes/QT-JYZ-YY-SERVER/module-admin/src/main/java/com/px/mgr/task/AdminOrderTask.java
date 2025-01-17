package com.px.mgr.task;

import com.px.db.model.bean.OrderInfo;
import com.px.db.model.bean.OrderSplit;
import com.px.db.model.bean.Store;
import com.px.db.model.service.OrderInfoService;
import com.px.db.model.service.OrderSplitService;
import com.px.db.model.service.SettingsService;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.util.DateUtil;
import com.px.web.service.XcxHelpService;
import com.px.web.util.common.PayMethodEnum;
import com.px.web.util.lakala.LakalaApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class AdminOrderTask {

    @Resource
    private OrderSplitService orderSplitService;
    @Resource
    private LakalaApiService lakalaApiService;
    @Resource
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void init() {

    }

    //订单微信分账
    @Scheduled(cron = "8 */1 * * * ?")//每×分钟运行一次
    public void orderSplitTask()  {
        QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
        searches.addQuery(OrderInfo.IS_SPLIT_FIELD,1);
        searches.addQuery(OrderInfo.SPLIT_STATUS_FIELD,0);

        searches.addQuery(OrderInfo.IS_CANCEL_FIELD,0);
        searches.addQuery(OrderInfo.PAY_STATUS_FIELD,2);
        searches.addQuery(OrderInfo.IS_RETURN_FIELD,0);

        List<OrderInfo> orderList = orderInfoService.selectByQuery(searches);
        if(orderList.size() == 0){
            return;
        }
        log.info("----------------订单分账--------------------------");
        int i = 0;
        for(OrderInfo o:orderList){
            if(o.getPayType()==3 ){
                //拉卡拉分账
                lakalaApiService.settle(o);
                i++;
            }
            if(i >= 30){
                break;
            }
        }
    }

    //订单微信分账
//    @Scheduled(cron = "18 */1 * * * ?")//每×分钟运行一次
//    public void orderSettleQueryTask()  {
//        QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
//        searches.addQuery(OrderInfo.IS_SPLIT_FIELD,1);
//        searches.addQuery(OrderInfo.SPLIT_STATUS_FIELD,1);
//        searches.addQuery(OrderInfo.PAY_TYPE_FIELD,3);
//
//        searches.addQuery(OrderInfo.IS_CANCEL_FIELD,0);
//        searches.addQuery(OrderInfo.PAY_STATUS_FIELD,2);
//        searches.addQuery(OrderInfo.IS_RETURN_FIELD,0);
//
//        List<OrderInfo> orderList = orderInfoService.selectByQuery(searches);
//        if(orderList.size() == 0){
//            return;
//        }
//        log.info("----------------订单结算查询--------------------------");
//        for(OrderInfo o:orderList){
//            if(o.getPayType()==3){
//                //拉卡拉分账
//                lakalaApiService.querySettle(o);
//            }
//        }
//    }

    //订单微信分账
    @Scheduled(cron = "46 */3 * * * ?")//每×分钟运行一次
    public void orderSplitQueryTask()  {
        List<OrderSplit> list = orderSplitService.selectEqualsT(new OrderSplit().setStatus(0));
        if(list.size() == 0){
            return;
        }
        log.info("----------------订单分账查询--------------------------");
        for(OrderSplit os:list){
            if(os.getPlatform().equals("lakala")){
                //微信支付调用微信分账
                lakalaApiService.separateQuery(os);
            }
        }
    }
}

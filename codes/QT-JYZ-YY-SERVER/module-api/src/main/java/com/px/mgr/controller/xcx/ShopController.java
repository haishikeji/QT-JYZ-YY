package com.px.mgr.controller.xcx;

import com.px.db.custom.service.NearbyStoreService;
import com.px.db.model.bean.AdminLog;
import com.px.db.model.bean.Store;
import com.px.db.model.bean.User;
import com.px.db.model.service.AdminLogService;
import com.px.db.model.service.SettingsService;
import com.px.db.model.service.UserService;
import com.px.web.service.XcxHelpService;
import com.px.util.MyExistUtil;
import com.px.web.controller.BaseController;
import com.px.web.util.AdminLog.AdminLogEnum;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.login.LoginUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/api/shop")
public class ShopController extends BaseController {
    @Resource
    private UserService userService;
    @Resource
    private XcxHelpService xcxHelpService;
    @Resource
    private SettingsService settingsService;
    @Resource
    private NearbyStoreService nearbyStoreService;
    @Resource
    private AdminLogService adminLogService;


    /**
     * 获取附近门店信息(不需要验证token)
     *
     * @param token
     * @param lat   纬度
     * @param lng   经度
     * @param page  分页数据
     * @return {
     * "data":{
     * "shopList":[
     * {
     * "id":10,  门店id
     * "contact_name":"联系人姓名",
     * "contact_mobile":"手机",
     * "username":"ceshi",//登录名称
     * "user_id":10011, //对应的门店管理员id
     * "status":1,
     * "create_at":"2021-03-08 10:35:00",创建时间
     * "lng":"118.058749124", 经度
     * "lat":"24.570556248",纬度
     * "shop_name":"测试油站【请勿下单】",  门店名称
     * "logo":"https://gas.ydunicorn.store/upload/59813d3db9ad9984/958013b3a72b1cc5.png",  门店logo
     * "address":"中国石油杏林加油站",  门店地址
     * "distance":"3.94km"  距离当前文章
     * },
     * {
     * "id":10,
     * *                 "shop_name":"测试油站【请勿下单】",
     * *                 "logo":"https://gas.ydunicorn.store/upload/59813d3db9ad9984/958013b3a72b1cc5.png",
     * *                 "address":"中国石油杏林加油站",
     * *                 "distance":"3.94km"
     * }
     * ],
     * "hasMore":false,  是否还有更多
     * "page":2  分页数据  如果 page为1 则返回2,page为2则返回3 ,一般每页取10个左右
     * }
     * }
     */
    @RequestMapping("/getList")
    @ResponseBody
    public ResponseResultUtil getList(String token, double lat, double lng, Integer page, Integer mchId) {
        log.info("-------------------收到小程序获取附近门店列表------------------------------");
        Integer maxDistance = 50000;
        String radius = settingsService.selectOneByProperty("code", "radius").getValue();
        if (MyExistUtil.notEmptyString(radius)) {
            maxDistance = Integer.parseInt(radius);
        }
        Map<String, Object> resutlMap = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        Integer limit = 10;
        //查询page
        Integer selectPage = (page - 1) * limit;
        List<Map<String, Object>> shopList = nearbyStoreService.getNearbyStore(maxDistance, mchId, lng, lat, null, null, selectPage, limit);
        for (Map<String, Object> objectMap : shopList) {
            if (MyExistUtil.notEmptyString(objectMap.get("logo").toString())) {
                objectMap.put("logo", xcxHelpService.getUrl() + objectMap.get("logo").toString());
            }
            objectMap.put("create_at", objectMap.get("create_time").toString());
            objectMap.put("lng", objectMap.get("lng").toString());
            objectMap.put("lat", objectMap.get("lat").toString());
            objectMap.put("distance", getDistanceStr(Double.parseDouble(objectMap.get("distance").toString())));
        }
        map.put("shopList", shopList);
        Integer count = nearbyStoreService.getNearbyStore(maxDistance, mchId, lng, lat, null, null, null, null).size();
        page += 1;
        boolean hasMore = false;
        if (count > page * 10) {
            hasMore = true;
        }
        map.put("hasMore", hasMore);
        map.put("page", page);
        resutlMap.put("data", map);

        //日志
        String name = "";
        User user = LoginUtil.getWechatUser(token);
        if (user != null) {
            name = user.getNickname();
        }
        String content = name + "获取附近门店 经度:+" + lng + "纬度:" + lat;
        AdminLog adminLog = new AdminLog();
        adminLog.setContent(content);
        adminLog.setAdminId(0).setAdminName("系统");
        adminLog.setTableName(Store.TABLE_NAME);
        adminLog.setTableNameSpace(Store.TABLE_NAME_SPACE);
        adminLog.setLogType(AdminLogEnum.OTHER.getLogType());
        adminLog.setLogTypeName(AdminLogEnum.OTHER.getLogTypeName());
        adminLogService.insertOne(adminLog);
        return ResponseResultUtil.success(resutlMap);
    }


    public String getDistanceStr(double distance) {
        String str = "";
        if (distance > 1000) {
            BigDecimal bigDecimal = new BigDecimal(distance).divide(new BigDecimal(1000), 2, BigDecimal.ROUND_HALF_UP);
            distance = bigDecimal.doubleValue();
            str = distance + "km";
        } else {
            distance = new BigDecimal(distance).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            str = distance + "m";
        }
        return str;
    }


}

package com.px.mgr.controller.xcx;

import com.alibaba.fastjson.JSONObject;
import com.px.db.custom.service.MainTotalService;
import com.px.db.model.bean.*;
import com.px.db.model.service.*;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.util.DateUtil;
import com.px.web.service.ApiHelpService;
import com.px.web.service.XcxHelpService;
import com.px.util.MyExistUtil;
import com.px.util.MyUuidUtil;
import com.px.web.controller.BaseController;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.login.LoginUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/api/user")
public class XcxLoginController extends BaseController {
    @Resource
    private UserService userService;
    @Resource
    private XcxHelpService xcxHelpService;
    @Resource
    private OrderInfoService orderInfoService;
    @Resource
    private OrderItemService orderItemService;
    @Resource
    private OrderReturnService orderReturnService;
    @Resource
    private UserGasCardService userGasCardService;
    @Resource
    private UserGasCardLogService userGasCardLogService;
    @Resource
    private UserIntegralService userIntegralService;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private MainTotalService mainTotalService;
    @Resource
    private StoreService storeService;
    @Resource
    private ApiHelpService apiHelpService;

    /**
     * 小程序授权登录
     *
     * @param code
     * @param user_info
     * @param encrypted_data
     * @param iv
     * @return
     */
    @RequestMapping("/login")
    @ResponseBody
    public ResponseResultUtil xcxLogin(String code, String user_info, String encrypted_data, String iv) {
        log.info("-------------------收到小程序授权登录------------------------------");
        log.info("-------------------收到参数------------------------------" + code);
        log.info("-------------------收到参数------------------------------" + user_info);
        //授权成功返回的数据
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", new HashMap<>());
        try {
            ResponseResultUtil responseResultUtil = xcxHelpService.getSessionKeyOrOpenId(code);
            if (responseResultUtil.getStatus() != 200) {
                return ResponseResultUtil.lose("授权登录异常");
            }
            JSONObject sessionKeyOpenId = (JSONObject) responseResultUtil.getData();
            if (sessionKeyOpenId == null) {
                return ResponseResultUtil.lose("授权登录异常");
            }
            if (!sessionKeyOpenId.containsKey("openid") || sessionKeyOpenId.get("openid") == null) {
                return ResponseResultUtil.lose("授权登录异常");
            }
            // 这里的ErrorCodeEnum是自定义错误字段，可以删除，用自己的方式处理
            // 获取openId && sessionKey
            String openId = sessionKeyOpenId.getString("openid");
            log.info("获取到openid：" + openId);
            if (openId == null) {
                return ResponseResultUtil.lose("授权登录异常");
            }
            Map map = new HashMap();
            String sessionKey = sessionKeyOpenId.getString("session_key");
            map.put("session_key", sessionKey);
            User info = JSONObject.parseObject(user_info, User.class);

            User user = userService.selectOneEqualsT(new User().setOpenId(openId));
            if (user == null) {
                if (MyExistUtil.isEmptyString(info.getMobile())) {
                    info.setMobile("");

                }
                String token = MyUuidUtil.getToken();
                info.setToken(token).setOpenId(openId);
                userService.insertOne(info);
                map.put("token", token);
                map.put("user_id", info.getId());
            } else {
                //更新
                info.setId(user.getId());
                if (MyExistUtil.notEmptyString(user.getAvatarUrl())) {
                    info.setAvatarUrl(user.getAvatarUrl());
                }
                if (MyExistUtil.notEmptyString(user.getNickname())) {
                    info.setNickname(user.getNickname());
                }
                if (!user.getOpenId().equals(openId)){
                    info.setOpenId(openId);
                }
                String mobile = "";
                if (MyExistUtil.notEmptyString(user.getMobile())) {
                    mobile = user.getMobile();
                }
                userService.updateByPrimaryKey(info);
                map.put("token", user.getToken());
                map.put("user_id", user.getId());
                map.put("mobile", mobile);
            }
            resultMap.put("data", map);

        } catch (Exception e) {
            log.info("登录异常：" + e.getMessage());
            return ResponseResultUtil.lose("授权登录异常");
        }

        return ResponseResultUtil.success(resultMap);

    }

    /**
     * 获取sessionKey
     *
     * @param map
     * @return
     */
    @RequestMapping(value = "/getSessionKey")
    @ResponseBody
    public ResponseResultUtil getsessionkey(@RequestBody Map map) {
        String sessionKey = "";
        String js_code = String.valueOf(map.get("code"));
        try {
            ResponseResultUtil responseResultUtil = xcxHelpService.getSessionKeyOrOpenId(js_code);
            if (responseResultUtil.getStatus() != 200) {
                return responseResultUtil;
            }
            JSONObject sessionKeyOpenId = (JSONObject) responseResultUtil.getData();
            if (sessionKeyOpenId == null) {
                return ResponseResultUtil.lose(1, "授权失败");
            }
            if (!sessionKeyOpenId.containsKey("openid") || sessionKeyOpenId.get("openid") == null) {
                return ResponseResultUtil.lose(1, "获取openId失败");
            }
            // 这里的ErrorCodeEnum是自定义错误字段，可以删除，用自己的方式处理
            // 获取openId && sessionKey
            sessionKey = sessionKeyOpenId.getString("session_key");
        } catch (Exception e) {
            return ResponseResultUtil.lose(1, "授权失败");
        }
        return ResponseResultUtil.successCode(sessionKey);

    }

    /**
     * 获取电话号码
     *
     * @param encryptedData
     * @param iv
     * @param se_key
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/decodePhone")
    @ResponseBody
    public String deciphering(String encryptedData, String iv, String se_key) throws Exception {
        byte[] encrypData = org.apache.commons.codec.binary.Base64.decodeBase64(String.valueOf(encryptedData));
        byte[] ivData = org.apache.commons.codec.binary.Base64.decodeBase64(String.valueOf(iv));
        byte[] sKey = Base64.decodeBase64(String.valueOf(se_key));
        String decrypt = decrypt(sKey, ivData, encrypData);
        return decrypt;

    }

    /**
     * 验证登录
     *
     * @param token
     * @return
     */
    @RequestMapping("/checkLogin")
    @ResponseBody
    public Map xcxLogin(String token) {
        log.info("-------------------收到小程序确认登录请求------------------------------");
        Map<String, Object> map = new HashMap<>();
        boolean flag = LoginUtil.haveWechatUser(token);
        if (flag) {
            map.put("code", 0);
            return map;
        }
        return map;
    }

    /**
     * 保存电话号码
     *
     * @param user_id 用户id
     * @param phone   电话号码
     * @return
     */
    @RequestMapping("/saveUserPhone")
    @ResponseBody
    public ResponseResultUtil saveUserPhone(Integer user_id, String phone) {
        log.info("----------------保存手机号");
        boolean flag = false;
        User user = userService.selectByPrimaryKey(user_id);
        if (user == null) {
            return ResponseResultUtil.lose(1, "该微信用户不存在");
        }
        if (MyExistUtil.notEmptyString(phone) && !phone.equals("undefined")) {
            //验证是否存在h5用户
            QueryHelper.SimpleQueryHandler handler = QueryHelper.startSimpleQuery();
            handler.addQuery(new QueryHelper.OrQueryModel("ifnull(open_id,'')", user.getOpenId(), QueryModel.NOT_EQUALS));
            handler.addQuery(new QueryHelper.OrQueryModel(User.MOBILE_FIELD, phone, QueryModel.EQUALS));
            List<User> userList = userService.selectByQuery(handler);
            if (userList.size() > 0) {
                log.info("--------------------存在h5账号,删除原创建xcx账号,合并账号-----------------");
                User exitUser = userList.get(0);
                //该电话号码已存在h5用户
                //创建需要删除的
                User deleteUser = new User().setId(user_id);
                User updatUser = new User().setId(exitUser.getId());
                updatUser.setOpenId(user.getOpenId()).setNickname(user.getNickname()).setRealname(user.getRealname())
                        .setAvatarUrl(user.getAvatarUrl()).setGender(user.getGender());
                //更新订单数据
                List<OrderInfo> orderInfos = orderInfoService.selectEqualsT(new OrderInfo().setUserId(deleteUser.getId()));
                if (orderInfos.size() > 0) {
                    List<OrderInfo> updatOrderInfoList = new ArrayList<>();
                    for (OrderInfo orderInfo : orderInfos) {
                        OrderInfo info = new OrderInfo().setUserId(exitUser.getId()).setId(orderInfo.getId());
                        info.setUserName(user.getNickname()).setMobile(phone);
                        updatOrderInfoList.add(info);
                    }
                    orderInfoService.updateBatch(updatOrderInfoList);
                }
                //更新退款数据
                List<OrderReturn> orderReturns = orderReturnService.selectEqualsT(new OrderReturn().setUserId(deleteUser.getId()));
                if (orderInfos.size() > 0) {
                    List<OrderReturn> updateOrderReturnList = new ArrayList<>();
                    for (OrderReturn orderReturn : orderReturns) {
                        OrderReturn returnOrder = new OrderReturn().setUserId(exitUser.getId()).setId(orderReturn.getId());
                        returnOrder.setUserName(user.getNickname()).setMobile(phone);
                        updateOrderReturnList.add(returnOrder);
                    }
                    orderReturnService.updateBatch(updateOrderReturnList);
                }
                //更新油卡数据
                List<UserGasCard> userGasCards = userGasCardService.selectEqualsT(new UserGasCard().setUserId(deleteUser.getId()));
                if (userGasCards.size() > 0) {
                    List<UserGasCard> userGasCardList = new ArrayList<>();
                    for (UserGasCard userGasCard : userGasCards) {
                        UserGasCard update = new UserGasCard();
                        update.setUserId(exitUser.getId()).setId(userGasCard.getId());
                        update.setNickname(user.getNickname()).setMobile(phone);
                        userGasCardList.add(update);

                    }
                    userGasCardService.updateBatch(userGasCardList);
                }
                //更新油卡交易记录
                List<UserGasCardLog> userGasCardLogs = userGasCardLogService.selectEqualsT(new UserGasCardLog().setUserId(deleteUser.getId()));
                if (userGasCardLogs.size() > 0) {
                    List<UserGasCardLog> userGasCardLogList = new ArrayList<>();
                    for (UserGasCardLog userGasCardLog : userGasCardLogs) {
                        UserGasCardLog update = new UserGasCardLog();
                        update.setUserId(exitUser.getId()).setId(userGasCardLog.getId());
                        update.setNickname(user.getNickname()).setMobile(phone);
                        userGasCardLogList.add(update);
                    }
                    userGasCardLogService.updateBatch(userGasCardLogList);
                }


                userService.deleteByPrimaryKey(deleteUser);
                flag = userService.updateByPrimaryKey(updatUser);
                LoginUtil.putWechatUser(exitUser.getToken());
                log.info("--------------------合并完成-----------------");
                log.info("--------------------合并结果-----------------" + flag);
                return flag ? ResponseResultUtil.successCode(exitUser.getToken()) : ResponseResultUtil.lose(1, "保存电话号码失败");
            } else {
                log.info("---------------------不存在h5账号,直接修改---------");
                flag = userService.updateByPrimaryKey(new User().setId(user_id).setMobile(phone));
                return flag ? ResponseResultUtil.successCode() : ResponseResultUtil.lose(1, "保存电话号码失败");
            }
        }
        return ResponseResultUtil.lose(1, "保存电话号码失败");
    }

    /**
     * 获取用户详情
     *
     * @param token   token
     * @param shop_id 门店id
     * @return
     */
    @RequestMapping("/detail")
    @ResponseBody
    public ResponseResultUtil getList(String token, Integer shop_id) {
        log.info("-------------------收到小程序获取detail------------------------------");
        log.info("-------------------参数------------------------------" + token);
        Map<String, Object> resutlMap = new HashMap<>();
        //
        User user = LoginUtil.getWechatUser(token);
        Map map = new HashMap();
        List<UserIntegral> userIntegralList = userIntegralService.selectEqualsT(new UserIntegral().setUserId(user.getId()));
        BigDecimal amount = BigDecimal.ZERO;
        for (UserIntegral userIntegral : userIntegralList) {
            amount = amount.add(userIntegral.getAmount());
        }

        User userInfo = userService.selectByPrimaryKey(user.getId());

        if (MyExistUtil.notEmptyString(userInfo.getAvatarUrl())) {
            if (!userInfo.getAvatarUrl().contains("https:")) {
                userInfo.setAvatarUrl(apiHelpService.getImageUrl(userInfo.getAvatarUrl()));
            }
        }

        userInfo.setBalance(amount);

        map.put("userInfo", userInfo.setPassword(""));
        /**
         * -----------------orderCount未设置----------------------------------
         */
        map.put("orderCount", new ArrayList<>());
        resutlMap.put("data", map);
        /**
         * ----------------判断是否为商家---------------------------------------
         */
        int business = 0;
        List<AdminUser> count = adminUserService.selectEqualsT(new AdminUser().setBindUserId(user.getId()));
        if (count.size() > 0) {
            business = 1;
        }
        resutlMap.put("business", business);
        return ResponseResultUtil.success(resutlMap);
    }

    @RequestMapping("xcxGetTotal")
    @ResponseBody
    public ResponseResultUtil xcxGetTotal(String token) {
        log.info("-------------------获取当日收款和订单数量---------------------");
        User user = LoginUtil.getWechatUser(token);
        if (user == null) {
            return ResponseResultUtil.lose("请重新登录");
        }
        List<AdminUser> adminUserList = adminUserService.selectEqualsT(new AdminUser().setBindUserId(user.getId()).setStatus((byte) 1));
        if (adminUserList.size() <= 0) {
            return ResponseResultUtil.success();
        }
        //获取StoreId
        List<Integer> adminIdList = adminUserList.stream().map(AdminUser::getStoreId).collect(Collectors.toList());
        List<Store> storeList = storeService.selectInProperty("id", adminIdList);
        if (storeList.size() <= 0) {
            return ResponseResultUtil.success();
        }
        List<Integer> storeIdList = storeList.stream().map(Store::getId).collect(Collectors.toList());
        //获取当天0点
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        long todayZero = startCalendar.getTimeInMillis();

        //获取当天最后时间点
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date());
        endCalendar.set(Calendar.HOUR_OF_DAY, 23);
        endCalendar.set(Calendar.MINUTE, 59);
        endCalendar.set(Calendar.SECOND, 59);
        long todayZero1 = endCalendar.getTimeInMillis();
        String today = DateUtil.FORMART.date2ShortString(new Date(todayZero));
        String today1 = DateUtil.FORMART.date2ShortString(new Date(todayZero1));
        String todayaaa = DateUtil.FORMART.date2ShortString(new Date());
        List<Map<String, Object>> map = mainTotalService.xcxGetMainTotal(storeIdList, todayaaa, todayaaa);


        if (map.size() <= 0) {
            for (Integer i : storeIdList) {
                Map<String, Object> map1 = new HashMap<>();
                map1.put("storeId", i);
                map1.put("oilTotal", 0);
                map1.put("oilTotalAmount", 0);
                map1.put("RechargeTotal", 0);
                map1.put("RechargeTotalAmount", 0);
                map.add(map1);
            }
        } else if (map.size() > 0 && map.size() < storeIdList.size()) {
            List<Integer> List = map.stream().map(e -> Integer.valueOf(e.get("storeId").toString())).collect(Collectors.toList());

            for (Integer ii : storeIdList) {
                for (Integer a : List) {
                    if (ii != a) {
                        Map<String, Object> map1 = new HashMap<>();
                        map1.put("storeId", ii);
                        map1.put("oilTotal", 0);
                        map1.put("oilTotalAmount", 0);
                        map1.put("RechargeTotal", 0);
                        map1.put("RechargeTotalAmount", 0);
                        map.add(map1);
                    }
                }
            }
        }
        for (Map map1 : map) {
            int type = 0;

            List<OrderReturn> returns = orderReturnService.selectEqualsT(new OrderReturn().setStoreId(Integer.valueOf(map1.get("storeId").toString()))
                    .setStatus((byte) 0).setIsCancel(false));

            if (returns.size() > 0) {
                type = 1;
            }
            map1.put("type", type);
            map1.put("storeName", storeService.selectByPrimaryKey(Integer.valueOf(map1.get("storeId").toString())).getTitle());
        }
        return ResponseResultUtil.success(map);
    }


    //    微信小程序
    public static String decrypt(byte[] key, byte[] iv, byte[] encData) throws Exception {
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        //解析解密后的字符串
        return new String(cipher.doFinal(encData), "UTF-8");
    }


    //@RequestMapping("/ceshi")
    //@ResponseBody
    //public ResponseResultUtil ceshi(String token) {
    //    boolean flag = LoginUtil.haveWechatUser(token);
    //    token = xcxHelpService.getToken();
    //    xcxHelpService.QR(token, "pages/teacher/today/today");
    //    if (flag) {
    //        return ResponseResultUtil.success("存在该微信用户");
    //    }
    //    return ResponseResultUtil.lose("该微信用户不存在");
    //}


    /**
     * 获取小程序用户信息
     *
     * @param token
     * @return
     */
    @RequestMapping("getXcxUser")
    @ResponseBody
    public ResponseResultUtil getXcxUser(String token) {
        log.info("----------获取小程序用户信息-------------");
        User user = LoginUtil.getWechatUser(token);
        if (user == null) {
            return ResponseResultUtil.lose(608, "请先登录");
        }
        User userInfo = userService.selectByPrimaryKey(user.getId());
        if (userInfo == null) {
            return ResponseResultUtil.lose("用户不存在或已删除");
        }
        String img = "";
        if (MyExistUtil.notEmptyString(userInfo.getAvatarUrl())) {
            img = userInfo.getAvatarUrl();
            if (!userInfo.getAvatarUrl().contains("https:")) {
                userInfo.setAvatarUrl(apiHelpService.getImageUrl(userInfo.getAvatarUrl()));
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("avatarUrl", userInfo.getAvatarUrl());
        map.put("name", userInfo.getNickname());
        map.put("img", img);
        return ResponseResultUtil.success(map);
    }

    /**
     * 修改用户信息
     *
     * @param token
     * @param img
     * @param name
     * @return
     */
    @RequestMapping("saveXcxUser")
    @ResponseBody
    public ResponseResultUtil saveXcxUser(String token, String img, String name) {
        log.info("--------------------修改用户信息-----------------------");
        User user = LoginUtil.getWechatUser(token);
        if (user == null) {
            return ResponseResultUtil.lose(608, "请先登录");
        }
        User userInfo = userService.selectByPrimaryKey(user.getId());
        if (userInfo == null) {
            return ResponseResultUtil.lose("用户不存在或已删除");
        }

        if (MyExistUtil.notEmptyString(img)) {
            userInfo.setAvatarUrl(img);
        }
        if (MyExistUtil.notEmptyString(name)) {
            userInfo.setNickname(name);
        }
        boolean flag = userService.updateByPrimaryKey(userInfo);
        return flag ? ResponseResultUtil.success() : ResponseResultUtil.lose();
    }

}

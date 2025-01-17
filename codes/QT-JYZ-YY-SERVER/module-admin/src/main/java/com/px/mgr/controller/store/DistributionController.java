package com.px.mgr.controller.store;

import com.px.db.custom.dao.MainTotalMapper;
import com.px.db.custom.service.MainTotalService;
import com.px.db.model.bean.Mch;
import com.px.db.model.bean.Store;
import com.px.db.model.service.MchService;
import com.px.db.model.service.StoreService;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.util.MyExistUtil;
import com.px.web.controller.BaseController;
import com.px.web.util.Nav.NavEnum;
import com.px.web.util.login.Authorization;
import com.px.web.util.login.PermissionEnum;
import com.px.web.util.login.PermissionGroupEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RequestMapping("store")
@Controller
@Slf4j
public class DistributionController extends BaseController {

    @Resource
    private MainTotalService mainTotalService;
    @Resource
    private MchService mchService;
    @Resource
    private StoreService storeService;

    private PermissionGroupEnum navGroup = PermissionGroupEnum.storeSelf;

    @RequestMapping("/distribution.html")
    @Authorization(PermissionEnum.storeList)
    public String selfInput(ModelMap model) {
        List<NavEnum> navList = NavEnum.getNavsByGroup(navGroup.name());
        model.put("navList", navList);
        model.put("navGroup", navGroup.getGroupName());
        model.put("navKey", NavEnum.storeDistribution.getKey());

        return "store/distribution";
    }

    @RequestMapping("/getStoreDistribution")
    @ResponseBody
    public List<Map<String,Object>> getStoreDistribution(Integer provinceId) {
        List<Map<String,Object>> list = mainTotalService.getSumStoreGroup(new Store().setProvinceId(provinceId).setMchId(0),"city");
        return list;
    }

    @RequestMapping("/statistics")
    @ResponseBody
    public ModelMap statistics(Integer mchId) {
        ModelMap map = new ModelMap();

        QueryHelper.SimpleQueryHandler searches = QueryHelper.startSimpleQuery();
        searches.addQuery(new QueryModel(Store.MCH_ID_PROPERTY,0));
        int storeCount = storeService.selectCountByQuery(searches);

        //订单统计
        map.put("store",mainTotalService.getSumStoreGroup(new Store().setMchId(0),"province"));
        map.put("storeCount",storeCount);

        return map;
    }
}

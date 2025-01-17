let App = getApp();

Page({

  /**
   * 页面的初始数据
   */
  data: {
    order_id: null,
    order: {},
    flag:true
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    this.data.order_id = options.order_id;
    this.getOrderDetail(options.order_id);
  },

  /**
   * 获取订单详情
   */
  getOrderDetail: function (order_id) {
    let _this = this;
    App._get1('order/orderDetail', {id:order_id }, function (result) {
      const order = result.data
      _this.setData({order});
    });
  },

  /**
   * 取消订单
   */
  cancelOrder: function (e) {
    let _this = this;
    let order_id = e.currentTarget.dataset.id;
    wx.showModal({
      title: "提示",
      content: "确认取消订单？",
      success: function (o) {
        if (o.confirm) {
        console.info("取消订单");
          App._post_form1('order/cancel', { order_id }, function (result) {
             if(result.status==608){
              App.showError('很抱歉，您还没有登录', function () {
                wx.navigateTo({
                    url: '../login/login',
                });
            });
             }else if(result.status!=200){
              App.showError(result.msg);
             }else{
              wx.showToast({
                title: '取消订单成功',
                icon: 'none',
                duration: 2000
              })
               _this.getOrderDetail(order_id);
             }
          });
        }
      }
    });
  },
   /**
   * 取消退款
   */
  cancelRefund: function (e) {
    let _this = this;
    let id = e.currentTarget.dataset.id;
    let order_id = e.currentTarget.dataset.oid;

    wx.showModal({
      title: "提示",
      content: "确认取消退款？",
      success: function (o) {
        if (o.confirm) {
          App._post_form1('order/returnCancel', { return_id:id }, function (result) {
             if(result.status==608){
              App.showError('很抱歉，您还没有登录', function () {
                wx.navigateTo({
                    url: '../login/login',
                });
            });
             }else if(result.status!=200){
              App.showError(result.msg);
             }else{
              wx.showToast({
                title: '取消退款成功',
                icon: 'none',
                duration: 2000
              })
              _this.getOrderDetail(order_id);
             }
          });
        }
      }
    });
  },
  continuePay: function (e) {
    let _this = this;
    if(_this.data.flag){
      _this.setData({
        flag:false
      })
       _this.payOrder(e);
    }
  },
  /**
   * 发起付款
   */
  payOrder: function (e) {
    let _this = this;
    let order_id = e.currentTarget.dataset.id;

    // 显示loading
    wx.showLoading({ title: '正在处理...', });
    App._post_form1('payment/pay', { order_id }, function (result) {
      if (result.status != 200) {
        App.showError(result.msg);
        _this.setData({
          flag:true
        })
        return false;
      }
      // 发起微信支付
      wx.requestPayment({
        timeStamp: result.data.timeStamp,
            nonceStr: result.data.nonceStr,
            package: result.data.package,
            signType: result.data.signType,
            paySign: result.data.sign,
        success: function (res) {
          wx.hideLoading()
          // 跳转到已付款订单
        wx.setStorage({key:"flag",data:"true"})
          wx.navigateTo({
            url: '../order/index'
          });
        },
        fail: function (res) {
          wx.hideLoading()
          App.showError('订单未支付');
        },
        complete:function(res){
          _this.setData({
           flag:true
         })
       }
      });
    });
  },

  /**
   * 确认收货
   */
  receipt: function (e) {
    let _this = this;
    let order_id = _this.data.order_id;
    wx.showModal({
      title: "提示",
      content: "确认收到商品？",
      success: function (o) {
        if (o.confirm) {
          App._post_form('user.order/receipt', { order_id }, function (result) {
            _this.getOrderDetail(order_id);
          });
        }
      }
    });
  },

  call() {
    const mobile = this.data.order.mobile
    if(mobile){
       wx.makePhoneCall({
      phoneNumber: mobile
    })
    }else{
      wx.showToast({
        title: '商家暂未提供',
        icon: 'none',
        duration: 2000
      })
    }
   
  },

});
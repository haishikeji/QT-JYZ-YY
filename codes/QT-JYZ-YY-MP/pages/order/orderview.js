const App = getApp();
var timer = 0;
Page({

  /**
   * 页面的初始数据
   */
  data: {
    order: {},
    store_id: 0, //门店id
    type: 1, //类型 1充值订单 2加油订单
    oil_id: 0, //油价id
    price: 0, //输入金额
    gun: '', //油枪
    card_id: 0, //油卡id
    order_from: 0, //订单来源（0小程序，1app，2后台补单）
    orderId: 0, //订单id
    flag: true,
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    const store_id = options.store_id;
    const type = options.type;
    const oil_id = options.oil_id;
    const price = options.price;
    const gun = options.gun;
    const card_id = options.card_id;

    this.setData({
      store_id: store_id,
      type: type,
      oil_id: oil_id,
      price: price,
      gun: gun,
      card_id: card_id,
    });
    this.orderConfirm();

  },
  /**
   * 订单预览
   */
  orderConfirm() {

    App._post_form1('order/orderConfirm', {
      oil_id: this.data.oil_id,
      gun: this.data.gun,
      shop_id: this.data.store_id,
      price: this.data.price,
      type: this.data.type,
    }, result => {
      if (result.status == 608) {
        App.showError('很抱歉，您还没有登录', function () {
          wx.navigateTo({
            url: '../login/login',
          });
        });
      } else if (result.status != 200) {
        App.showError(result.msg, function () {
          wx.navigateTo({
            url: '../gas/index?shop_id=' + this.data.shop_id + '&oil_id=' + this.data.oil_id,
          });
        });
      } else {
        const order = result.data;
        this.setData({
          order: order
        });
      }


    });
  },

  /**
   * 创建订单
   */
  createOrder() {
    if (this.data.flag) {
      this.setData({
        flag: false
      })
      if(this.data.orderId > 0){
        this.payOrder(this.data.orderId);
      }else if (this.data.type == 1) {
        this.createRechargeOrder();
      } else {
        this.createOilOrder();
      }
    }



  },
  /**
   *订单支付 
   */
  payOrder: function (order_id) {
    let _this = this;
    App._get1('order/orderDetail', {
      id: order_id
    }, function (result) {
      if (result.status == 608) {
        App.showError('很抱歉，您还没有登录', function () {
          wx.navigateTo({
            url: '../login/login',
          });
        });
      } else if (result.status != 200) {
        App.showError(result.msg);
      } else {
        console.info("订单详细数据");
        if (result.data.pay_price > 0) {
          //支付

          // 显示loading
          wx.showLoading({
            title: '正在处理...',
          });
          App._post_form1('payment/pay', {
            order_id
          }, function (result) {
            _this.setData({
              flag: true
            })
            let payData = result.data;
            if(typeof payData.payType != 'undefined' && payData.payType == 'lakala'){
              // 2、打开收银台半屏小程序示例代码
              wx.openEmbeddedMiniProgram({
                appId: 'wxc3e4d1682da3053c',
                path: `payment-cashier/pages/checkout/index?source=WECHATMINI&counterUrl=${encodeURIComponent(payData.counterUrl)}`,
                envVersion: 'release',  // release: 正式版  trial: 体验版
                success(res) {
                  // 打开成功：轮询订单支付状态
                  _this.queryOrder(order_id);
                },fail(res){
                  //打开失败
                  wx.hideLoading()
                  // App.showError('订单未支付');
                  wx.showToast({
                    title: '订单未支付',
                    icon: 'none',
                    duration: 1000
                  })
                  wx.setStorage({
                    key: "flag",
                    data: "false"
                  })
                  setTimeout(() => {
                    wx.redirectTo({
                      url: '/pages/order/index'
                    });
                  }, 1000)
                }
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
                wx.showToast({
                  title: '支付成功',
                  icon: 'none',
                  duration: 1000
                })
                wx.setStorage({
                  key: "flag",
                  data: "false"
                })

                setTimeout(() => {
                  wx.redirectTo({
                    url: '/pages/order/index'
                  });
                }, 1000)
              },
              fail: function (res) {
                wx.hideLoading()
                // App.showError('订单未支付');
                wx.showToast({
                  title: '订单未支付',
                  icon: 'none',
                  duration: 1000
                })
                wx.setStorage({
                  key: "flag",
                  data: "false"
                })
                setTimeout(() => {
                  wx.redirectTo({
                    url: '/pages/order/index'
                  });
                }, 1000)

              },
            });
          });
        } else {
          //0元支付
          App._post_form1('order/zeroPay', {
            order_id
          }, function (result) {
            if (result.status != 200) {
              App.showError(result.msg);
              return false;
            } else {
              wx.showToast({
                title: '支付成功',
                icon: 'success',
                duration: 2000
              })
              wx.setStorage({
                key: "flag",
                data: "false"
              })

              setTimeout(() => {
                wx.redirectTo({
                  url: '/pages/order/index'
                });
              }, 1000)
            }


          });

        }

      }

    });
  },

  queryOrder:function(orderId){
    clearInterval(timer);
    var count = 0;
    timer = setInterval(function(){
       //轮询60次没有结束，自动停止轮询
       if(count >= 60){
        wx.redirectTo({
          url: '/pages/order/index'
        });
      }
      App._post_form1('payment/checkPay', {
        orderId:orderId
      }, function (res) {
        count++;
        if(res.data == '1'){
          clearInterval(timer);
          wx.showToast({
            title: '支付成功',
            icon: 'none',
            duration: 1000
          })
          wx.setStorage({
            key: "flag",
            data: "false"
          })

          setTimeout(() => {
            wx.redirectTo({
              url: '/pages/order/index'
            });
          }, 1000)
        }

      });
    },1500);
  },

  /**
   * 创建加油订单
   */
  createOilOrder() {
    App._post_form1('order/buyNow2', {
      oil_id: this.data.oil_id,
      gun: this.data.gun,
      shop_id: this.data.store_id,
      price: this.data.price,
      type: this.data.type,
      order_from: this.data.order_from
    }, result => {
      if (result.status == 608) {
        App.showError('很抱歉，您还没有登录', function () {
          wx.navigateTo({
            url: '../login/login',
          });
        });
      } else if (result.status != 200) {
        App.showError(result.msg);
      } else {
        this.setData({orderId:result.data});
        this.payOrder(result.data);
      }
    },result=>{
      this.setData({
        flag: true
      })
    }
    )
  },
  /**
   * 创建充值订单
   */
  createRechargeOrder() {
    App._post_form1('order/recharge', {
      cat_id: this.data.card_id,
      price_total: this.data.price,
      order_from: this.data.order_from
    }, result => {
      if (result.status == 608) {
        App.showError('很抱歉，您还没有登录', function () {
          wx.navigateTo({
            url: '../login/login',
          });
        });
      } else if (result.status != 200) {
        App.showError(result.msg);
      } else {
        this.setData({orderId:result.data});
        this.payOrder(result.data);
      }
    },result=>{
      this.setData({
        flag: true
      })
    }
  );
  },
  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady: function () {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide: function () {},

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload: function () {
    //释放定时器
    clearInterval(timer);
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function () {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {

  }
})
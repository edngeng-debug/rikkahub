/* 大肥鱼嵌入版：挂件本体只负责交互；设置由 RikkaHub「插件 > 大肥鱼」管理。 */
(function(){
  if(window.__dshwDebugUi)return;
  window.__dshwDebugUi=true;

  var timer=0,lastPanelOpen=null,nativeDrag=null,root=null;
  var PANEL_SELECTOR='.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask,.dshwv-qedit,.dshwv-usagepanel,.dshwv-rolelist,.dshwv-audiolist,.dshwv-custmenu,.dshwv-hintbox';

  function isVisible(el){
    if(!el)return false;
    try{
      var c=getComputedStyle(el),r=el.getBoundingClientRect();
      return c.display!=='none'&&c.visibility!=='hidden'&&r.width>1&&r.height>1;
    }catch(e){return false}
  }

  function panelIsOpen(){
    try{
      var all=document.querySelectorAll(PANEL_SELECTOR);
      for(var i=0;i<all.length;i++)if(isVisible(all[i]))return true;
    }catch(e){}
    return false;
  }

  function setPanelOpen(v){
    if(!window.RikkaDaFeiYu||lastPanelOpen===v)return;
    lastPanelOpen=v;
    try{window.RikkaDaFeiYu.setPanelOpen(v)}catch(e){}
  }

  function sync(){
    try{
      var menu=document.querySelector('.dshwv-menu');
      if(menu)menu.style.setProperty('display','none','important');
      var buttons=document.querySelectorAll('.dshwv-menu-btn,.dshwv-menu-btn-visible');
      for(var i=0;i<buttons.length;i++)buttons[i].style.setProperty('display','none','important');
      setPanelOpen(panelIsOpen());
    }catch(e){}
  }

  function schedule(){
    if(timer)return;
    timer=setTimeout(function(){timer=0;sync()},20);
  }

  function pinRoot(left,top){
    var r=document.querySelector('.dshwv-root');
    if(!r)return;
    r.style.setProperty('left',left+'px','important');
    r.style.setProperty('top',top+'px','important');
    r.style.setProperty('right','auto','important');
    r.style.setProperty('bottom','auto','important');
  }
  window.__dshwPinRoot=pinRoot;

  function directPanel(action){
    var f=null;
    try{
      if(action==='bubble' && typeof openBubbleEditor==='function')f=openBubbleEditor;
      else if(action==='role' && typeof toggleRolePanel==='function')f=toggleRolePanel;
      else if(action==='snap' && typeof openSnapModal==='function')f=openSnapModal;
      else if(action==='turn' && typeof usageAlertBudgetEditor==='function')f=function(){usageAlertBudgetEditor('cost',null)};
      else if(action==='audio' && typeof toggleAudioGroupPanel==='function')f=toggleAudioGroupPanel;
    }catch(e){f=null}
    if(!f)return false;
    try{f();return true}catch(e){return false}
  }

  function consumePendingPanel(){
    if(!window.RikkaDaFeiYu)return;
    var action='';
    try{action=window.RikkaDaFeiYu.consumePanelAction()||''}catch(e){}
    if(!action)return;
    setTimeout(function(){
      if(directPanel(action)){sync();return}
      // whale-widget.js 可能尚未完成函数初始化，稍后重试一次。
      setTimeout(function(){directPanel(action);sync()},250);
    },80);
  }

  // 原挂件的 pressDown 会产生 Q 弹/缩放；嵌入 RikkaHub 时点击不再抖动。
  function disablePressSquish(){
    try{
      var st=document.createElement('style');
      st.className='dshwv-embedded-touch-style';
      st.textContent='.dshwv-body{transform:none!important}.dshwv-root{transition:none!important}';
      document.head.appendChild(st);
    }catch(e){}
  }

  // JS 仍然负责识别“这一下确实点到了鲸鱼并进入原生 drag”，但实际位移交给 RikkaHub。
  // 这样不再把 WebView 扩成全屏，也不会因为网页 viewport 只有 250px 而卡在左上角。
  function installNativeDragBridge(){
    if(!root)return;
    document.addEventListener('pointerdown',function(e){
      try{
        if(root.classList.contains('dshwv-dragging'))nativeDrag={x:e.clientX,y:e.clientY};
        else nativeDrag=null;
      }catch(err){nativeDrag=null}
    },true);

    document.addEventListener('pointermove',function(e){
      if(!nativeDrag)return;
      try{
        if(!root.classList.contains('dshwv-dragging'))return;
        var dx=e.clientX-nativeDrag.x,dy=e.clientY-nativeDrag.y;
        nativeDrag.x=e.clientX;nativeDrag.y=e.clientY;
        if((dx||dy)&&window.RikkaDaFeiYu){
          try{window.RikkaDaFeiYu.moveWidgetBy(dx,dy)}catch(err){}
          // 把网页内 root 固定在左上附近，防止原网页自身的 250px 视口限制住移动距离。
          pinRoot(5,5);
        }
      }catch(err2){}
    },true);

    var endNativeDrag=function(){nativeDrag=null;setTimeout(sync,0)};
    document.addEventListener('pointerup',endNativeDrag,true);
    document.addEventListener('pointercancel',endNativeDrag,true);
  }

  function install(){
    root=document.querySelector('.dshwv-root');
    if(!root){setTimeout(install,120);return}
    if(document.querySelector('.dshwv-embedded-settings-style')){consumePendingPanel();sync();return}

    var st=document.createElement('style');
    st.className='dshwv-embedded-settings-style';
    st.textContent=[
      'html,body,#root{width:100%!important;height:100%!important;margin:0!important;overflow:visible!important}',
      '.dshwv-root{--dshw-base:250px!important;width:250px!important;height:250px!important;left:5px!important;top:5px!important;right:auto!important;bottom:auto!important}',
      '.dshwv-root.dshwv-left .dshwv-img{transform:scaleX(-1)!important;transform-origin:right bottom!important}',
      '.dshwv-root.dshwv-left .dshwv-gif{transform:translate(-50%,-50%) scaleX(-1)!important}',
      '.dshwv-root.dshwv-left .dshwv-text{transform:translate(-50%,-50%)!important}',
      '.dshwv-menu,.dshwv-menu-btn,.dshwv-menu-btn-visible{display:none!important}',
      '.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask{position:fixed!important;left:0!important;top:0!important;right:0!important;bottom:0!important;width:100vw!important;height:100vh!important;min-width:100vw!important;min-height:100vh!important;max-width:none!important;max-height:none!important;box-sizing:border-box!important;align-items:center!important;justify-content:center!important;overflow:auto!important;z-index:100001!important}',
      '.dshwv-bubcard{width:min(440px,calc(100vw - 24px))!important;max-width:calc(100vw - 24px)!important;max-height:88vh!important;overflow-y:auto!important;overflow-x:hidden!important;box-sizing:border-box!important;flex:0 0 auto!important}',
      '.dshwv-rolelist,.dshwv-audiolist,.dshwv-custmenu,.dshwv-hintbox,.dshwv-qedit,.dshwv-usagepanel{z-index:100002!important;max-width:calc(100vw - 24px)!important}'
    ].join('');
    document.head.appendChild(st);
    disablePressSquish();
    installNativeDragBridge();

    try{
      new MutationObserver(schedule).observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style','hidden']});
    }catch(e){}

    consumePendingPanel();
    sync();
    setInterval(function(){consumePendingPanel();sync()},300);
  }

  install();
})();

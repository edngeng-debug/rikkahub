/* 大肥鱼嵌入版：聊天页不再显示调试菜单，设置统一放到「插件 > 大肥鱼」。 */
(function(){
  if(window.__dshwDebugUi)return;
  window.__dshwDebugUi=true;

  var nativeSettings=window.__RIKKAHUB_DAFEIYU_SETTINGS||{};
  var state={
    char:Number(nativeSettings.char)||1,
    bubble:Number(nativeSettings.bubble)||1,
    label:Number(nativeSettings.label)||1,
    amount:Number(nativeSettings.amount)||1,
    hint:Number(nativeSettings.hint)||1
  };
  var observerTimer=0;
  var lastInteractive=null;
  var lastRect=null;

  function setInteractive(v){
    if(!window.RikkaDaFeiYu||lastInteractive===v)return;
    lastInteractive=v;
    try{window.RikkaDaFeiYu.setInteractive(v)}catch(e){}
  }

  function reportRect(){
    try{
      var r=document.querySelector('.dshwv-root');
      if(!r||!window.RikkaDaFeiYu)return;
      var b=r.getBoundingClientRect();
      var next=[b.left,b.top,b.right,b.bottom,window.innerWidth,window.innerHeight];
      if(lastRect&&next.every(function(v,i){return i===4||i===5?v===lastRect[i]:Math.abs(v-lastRect[i])<.5}))return;
      lastRect=next;
      window.RikkaDaFeiYu.setWidgetRect.apply(window.RikkaDaFeiYu,next);
    }catch(e){}
  }

  function sync(){
    try{
      var open=!!document.querySelector('.dshwv-menu.dshwv-menu-open,.dshwv-debug-mask,.dshwv-root.dshwv-dragging');
      if(!open){
        var n=document.querySelectorAll('.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask,.dshwv-qedit,.dshwv-usagepanel,.dshwv-rolelist,.dshwv-audiolist,.dshwv-custmenu');
        for(var i=0;i<n.length;i++){
          var e=n[i],c=getComputedStyle(e),r=e.getBoundingClientRect();
          if(c.display!=='none'&&c.visibility!=='hidden'&&r.width>1&&r.height>1){open=true;break}
        }
      }
      setInteractive(open);
      reportRect();
    }catch(e){}
  }

  function scheduleSync(){
    if(observerTimer)return;
    observerTimer=setTimeout(function(){observerTimer=0;sync()},60);
  }

  function baseFont(el){
    if(!el.dataset.dshwBaseFont)el.dataset.dshwBaseFont=getComputedStyle(el).fontSize;
    var n=parseFloat(el.dataset.dshwBaseFont);
    return isFinite(n)?n:0;
  }

  function apply(){
    document.querySelectorAll('.dshwv-img').forEach(function(el){
      el.style.setProperty('--dshw-debug-char-scale',state.char,'important');
    });
    document.querySelectorAll('.dshwv-pop').forEach(function(el){
      el.style.setProperty('--dshw-debug-bubble-scale',state.bubble,'important');
    });
    document.querySelectorAll('.dshwv-label').forEach(function(el){
      el.style.setProperty('font-size',(baseFont(el)*state.label)+'px','important');
    });
    document.querySelectorAll('.dshwv-amount').forEach(function(el){
      el.style.setProperty('font-size',(baseFont(el)*state.amount)+'px','important');
    });
    document.querySelectorAll('.dshwv-hint,.dshwv-period').forEach(function(el){
      el.style.setProperty('font-size',(baseFont(el)*state.hint)+'px','important');
    });
    scheduleSync();
  }

  function installStyle(){
    if(document.querySelector('.dshwv-embedded-settings-style'))return;
    var st=document.createElement('style');
    st.className='dshwv-embedded-settings-style';
    st.textContent=[
      '.dshwv-root.dshwv-left{transform:none!important}',
      '.dshwv-root.dshwv-left .dshwv-img{transform:scaleX(-1) scale(var(--dshw-debug-char-scale,1))!important;transform-origin:right bottom!important}',
      '.dshwv-root.dshwv-left .dshwv-gif{transform:translate(-50%,-50%) scaleX(-1)!important}',
      '.dshwv-root.dshwv-left .dshwv-text{transform:translate(-50%,-50%)!important}',
      '.dshwv-pop{transform:scale(var(--dshw-debug-bubble-scale,1))!important;transform-origin:44.25% 36%!important}',
      '.dshwv-menu-btn,.dshwv-menu-btn-visible{display:none!important;visibility:hidden!important;pointer-events:none!important}',
      '.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;box-sizing:border-box!important;align-items:center!important;justify-content:center!important;overflow:auto!important}',
      '.dshwv-bubcard{width:min(440px,calc(100vw - 24px))!important;max-height:88vh!important;overflow-y:auto!important;overflow-x:hidden!important;box-sizing:border-box!important}'
    ].join('');
    document.head.appendChild(st);
  }

  function wait(){
    var r=document.querySelector('.dshwv-root');
    if(!r){setTimeout(wait,200);return;}
    installStyle();
    apply();
    sync();
    try{
      new MutationObserver(scheduleSync).observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style','hidden']});
    }catch(e){}
    setInterval(sync,1000);
  }

  wait();
})();

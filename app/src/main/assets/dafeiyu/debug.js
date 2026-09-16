/* 大肥鱼嵌入版：只负责把原版网页挂件的交互桥接到 RikkaHub。 */
(function(){
  if(window.__dshwDebugUi)return;
  window.__dshwDebugUi=true;

  var root=null;
  var drag=null;
  var MOVE_THRESHOLD=6;
  var PANEL_SELECTOR='.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask,.dshwv-qedit,.dshwv-usagepanel,.dshwv-rolelist,.dshwv-audiolist,.dshwv-custmenu,.dshwv-hintbox';

  function visible(el){
    if(!el)return false;
    try{
      var c=getComputedStyle(el),r=el.getBoundingClientRect();
      return c.display!=='none'&&c.visibility!=='hidden'&&r.width>1&&r.height>1;
    }catch(e){return false}
  }

  function sync(){
    try{
      var menu=document.querySelector('.dshwv-menu');
      if(menu)menu.style.setProperty('display','none','important');
      var buttons=document.querySelectorAll('.dshwv-menu-btn,.dshwv-menu-btn-visible');
      for(var i=0;i<buttons.length;i++)buttons[i].style.setProperty('display','none','important');
    }catch(e){}
  }

  function installTouchBridge(){
    if(!root)return;
    var body=root.querySelector('.dshwv-body')||root;
    body.style.setProperty('pointer-events','auto','important');
    body.style.setProperty('touch-action','none','important');
    root.style.setProperty('pointer-events','auto','important');

    document.addEventListener('pointerdown',function(e){
      try{
        if(!root.contains(e.target))return;
        if(visible(document.querySelector(PANEL_SELECTOR)))return;
        drag={x:e.clientX,y:e.clientY,lastX:e.clientX,lastY:e.clientY,moved:false,pointerId:e.pointerId};
        e.stopImmediatePropagation();
      }catch(err){drag=null}
    },true);

    document.addEventListener('pointermove',function(e){
      if(!drag||e.pointerId!==drag.pointerId)return;
      try{
        var total=Math.hypot(e.clientX-drag.x,e.clientY-drag.y);
        if(!drag.moved && total<MOVE_THRESHOLD){
          e.stopImmediatePropagation();
          return;
        }
        drag.moved=true;
        var dx=e.clientX-drag.lastX,dy=e.clientY-drag.lastY;
        drag.lastX=e.clientX;drag.lastY=e.clientY;
        if((dx||dy)&&window.RikkaDaFeiYu)window.RikkaDaFeiYu.moveWidgetBy(dx,dy);
        e.preventDefault();
        e.stopImmediatePropagation();
      }catch(err){}
    },true);

    document.addEventListener('pointerup',function(e){
      if(!drag||e.pointerId!==drag.pointerId)return;
      var wasMoved=drag.moved;
      drag=null;
      try{
        e.stopImmediatePropagation();
        if(wasMoved)e.preventDefault();
      }catch(err){}
    },true);
    document.addEventListener('pointercancel',function(e){
      if(drag&&e.pointerId===drag.pointerId)drag=null;
    },true);
  }

  function install(){
    root=document.querySelector('.dshwv-root');
    if(!root){setTimeout(install,120);return}
    if(!document.querySelector('.dshwv-embedded-settings-style')){
      var st=document.createElement('style');
      st.className='dshwv-embedded-settings-style';
      st.textContent=[
        'html,body,#root{width:100%!important;height:100%!important;margin:0!important;overflow:visible!important}',
        '.dshwv-root{--dshw-base:250px!important;width:250px!important;height:250px!important;left:0!important;top:0!important;right:auto!important;bottom:auto!important;transition:none!important;pointer-events:auto!important}',
        '.dshwv-body{transform:none!important;animation:none!important;transition:none!important;pointer-events:auto!important;touch-action:none!important}',
        '.dshwv-menu,.dshwv-menu-btn,.dshwv-menu-btn-visible{display:none!important}',
        '.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask{position:absolute!important;z-index:100001!important;box-sizing:border-box!important}',
        '.dshwv-rolelist,.dshwv-audiolist,.dshwv-custmenu,.dshwv-qedit,.dshwv-usagepanel,.dshwv-hintbox{position:absolute!important;z-index:100002!important;box-sizing:border-box!important}',
        '.dshwv-img,.dshwv-gif{user-select:none!important;-webkit-user-drag:none!important}'
      ].join('');
      document.head.appendChild(st);
    }
    installTouchBridge();
    sync();
    setInterval(sync,1000);
  }
  install();
})();

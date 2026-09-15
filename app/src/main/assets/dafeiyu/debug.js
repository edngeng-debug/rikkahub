(function(){
  if (window.__dshwDebugUi) return;
  window.__dshwDebugUi=true;

  function syncInteractive(){
    try{
      if(!window.RikkaDaFeiYu) return;
      var active=!!document.querySelector('.dshwv-menu.dshwv-menu-open,.dshwv-debug-mask');
      if(!active){
        var nodes=document.querySelectorAll('body *');
        for(var i=0;i<nodes.length;i++){
          var el=nodes[i];
          if(el.closest && el.closest('.dshwv-root')) continue;
          var cs=getComputedStyle(el), r=el.getBoundingClientRect();
          var zi=parseInt(cs.zIndex,10);
          if(cs.position==='fixed' && zi>=10000 && r.width>0 && r.height>0 && cs.display!=='none' && cs.visibility!=='hidden'){
            active=true; break;
          }
        }
      }
      window.RikkaDaFeiYu.setInteractive(active);
    }catch(e){}
  }

  function observe(){
    try{
      new MutationObserver(syncInteractive).observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style','hidden']});
      syncInteractive();
    }catch(e){}
  }

  function wait(){
    var menu=document.querySelector('.dshwv-menuview');
    var root=document.querySelector('.dshwv-root');
    if(!menu||!root){setTimeout(wait,300);return}
    inject(menu,root);
    observeMenu();
    observe();
  }

  function observeMenu(){
    try{
      var menu=document.querySelector('.dshwv-menu');
      if(!menu) return;
      var sync=function(){try{
        if(window.RikkaDaFeiYu) window.RikkaDaFeiYu.setMenuOpen(menu.classList.contains('dshwv-menu-open'));
      }catch(e){}};
      new MutationObserver(sync).observe(menu,{attributes:true,attributeFilter:['class']});
      sync();
    }catch(e){}
  }

  function inject(menu,root){
    if(document.querySelector('.dshwv-debug-open')) return;

    var style=document.createElement('style');
    style.id='dshwv-debug-style';
    style.textContent=[
      'html,body{width:100%!important;height:100%!important;min-width:100%!important;min-height:100%!important;margin:0!important;overflow:visible!important}',
      'html,body,#root{box-sizing:border-box!important}',
      '.dshwv-debug-open{flex:1;border:1px solid rgba(32,49,112,.4);border-radius:6px;background:rgba(32,49,112,.08);color:#203170;font-size:12px;padding:4px 8px;cursor:pointer}',
      '.dshwv-debug-open:hover{background:rgba(32,49,112,.16)}',
      '.dshwv-debug-mask{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;min-width:100vw!important;min-height:100vh!important;max-width:none!important;max-height:none!important;background:rgba(15,23,42,.55);z-index:31000;display:flex!important;align-items:center;justify-content:center;color-scheme:light;box-sizing:border-box!important;overflow:auto!important}',
      '.dshwv-debug-card{width:min(390px,92vw)!important;height:auto!important;min-height:0!important;max-height:86vh!important;overflow:auto!important;flex:0 0 auto!important;background:#fff;border-radius:12px;padding:16px;box-shadow:0 10px 30px rgba(0,0,0,.3);color:#203170;box-sizing:border-box!important}',
      '.dshwv-debug-title{font-size:15px;font-weight:700;text-align:center;margin-bottom:12px}',
      '.dshwv-debug-row{display:flex;align-items:center;gap:8px;margin:9px 0;font-size:12px;height:auto!important;min-height:0!important}',
      '.dshwv-debug-row label{width:72px;flex:0 0 72px}',
      '.dshwv-debug-row input[type=range]{flex:1;min-width:0}',
      '.dshwv-debug-num{width:58px;box-sizing:border-box;border:1px solid rgba(32,49,112,.4);border-radius:6px;padding:3px 5px;color:#203170;background:#fff}',
      '.dshwv-debug-btns{display:flex;gap:10px;justify-content:center;margin-top:14px}',
      '.dshwv-debug-btn{border:0;border-radius:6px;padding:7px 18px;font-size:13px;cursor:pointer}',
      '.dshwv-debug-ok{background:#203170;color:#fff}.dshwv-debug-reset{background:rgba(32,49,112,.1);color:#203170}',
      '.dshwv-root{--dshw-debug-char:1;--dshw-debug-bubble:1;--dshw-debug-label:1;--dshw-debug-amount:1;--dshw-debug-hint:1;--dshw-debug-button:1}',
      '.dshwv-img{width:calc(59.45% * var(--dshw-debug-char,1))!important;height:calc(59.45% * var(--dshw-debug-char,1))!important}',
      '.dshwv-pop{scale:var(--dshw-debug-bubble,1)!important}',
      '.dshwv-label{scale:var(--dshw-debug-label,1)!important}',
      '.dshwv-amount{scale:var(--dshw-debug-amount,1)!important}',
      '.dshwv-hint{scale:var(--dshw-debug-hint,1)!important}',
      '.dshwv-menu-btn{scale:var(--dshw-debug-button,1)!important}',
      
      /* 二级 UI：所有弹窗都脱离挂件根节点，强制拥有真实的视口尺寸。 */
      '.dshwv-bubmask,.dshwv-audiomask,.dshwv-confirmmask,.dshwv-cropmask,.dshwv-snapmask,.dshwv-resmask,.dshwv-usage-mask{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;min-width:100vw!important;min-height:100vh!important;max-width:none!important;max-height:none!important;box-sizing:border-box!important;display:flex!important;align-items:center!important;justify-content:center!important;overflow:auto!important}',
      '.dshwv-bubmask[style*="display: none"],.dshwv-audiomask[style*="display: none"],.dshwv-confirmmask[style*="display: none"],.dshwv-cropmask[style*="display: none"],.dshwv-snapmask[style*="display: none"],.dshwv-resmask[style*="display: none"],.dshwv-usage-mask[style*="display: none"]{display:none!important}',
      '.dshwv-bubcard{width:min(440px,calc(100vw - 24px))!important;height:auto!important;min-height:120px!important;max-height:88vh!important;overflow-y:auto!important;overflow-x:hidden!important;flex:0 0 auto!important;box-sizing:border-box!important;display:block!important}',
      '.dshwv-bubcard>*{height:auto!important;min-height:0!important;box-sizing:border-box!important}',
      '.dshwv-bubtitle{display:block!important;height:auto!important;min-height:20px!important;line-height:20px!important}',
      '.dshwv-bubrow,.dshwv-bubsec,.dshwv-bubpal,.dshwv-bubpvbox,.dshwv-bubprev,.dshwv-bubbtns{height:auto!important;min-height:0!important}',
      '.dshwv-qedit,.dshwv-usagepanel{height:auto!important;min-height:0!important;max-height:88vh!important;overflow:auto!important;box-sizing:border-box!important}',
      '.dshwv-root.dshwv-left .dshwv-text{transform:translate(-50%,-50%) scaleX(-1)!important}',
      '.dshwv-root.dshwv-left .dshwv-text *{transform:none!important}',
      '.dshwv-root.dshwv-left .dshwv-label,.dshwv-root.dshwv-left .dshwv-amount,.dshwv-root.dshwv-left .dshwv-hint,.dshwv-root.dshwv-left .dshwv-period{transform:none!important}'
    ].join('');
    document.head.appendChild(style);

    var row=document.createElement('div'); row.className='dshwv-menu-row';
    var lab=document.createElement('span'); lab.textContent='组件大小';
    var btn=document.createElement('button'); btn.className='dshwv-debug-open'; btn.textContent='调试'; btn.type='button';
    row.appendChild(lab); row.appendChild(btn);
    var rows=menu.querySelectorAll('.dshwv-menu-row');
    var anchor=rows.length?rows[rows.length-1]:null;
    if(anchor&&anchor.parentNode) anchor.parentNode.insertBefore(row,anchor.nextSibling); else menu.appendChild(row);

    var key='dshw-debug-size-v2';
    var defaults={char:1,bubble:1,label:1,amount:1,hint:1,button:1};
    function load(){try{return Object.assign({},defaults,JSON.parse(localStorage.getItem(key)||'{}'))}catch(e){return Object.assign({},defaults)}}
    function save(s){try{localStorage.setItem(key,JSON.stringify(s))}catch(e){}}
    function apply(s){
      root.style.setProperty('--dshw-debug-char',s.char);
      root.style.setProperty('--dshw-debug-bubble',s.bubble);
      root.style.setProperty('--dshw-debug-label',s.label);
      root.style.setProperty('--dshw-debug-amount',s.amount);
      root.style.setProperty('--dshw-debug-hint',s.hint);
      root.style.setProperty('--dshw-debug-button',s.button);
    }
    var current=load(); apply(current);
    btn.addEventListener('click',function(e){e.stopPropagation();open(current)});

    function open(state){
      var mask=document.createElement('div'); mask.className='dshwv-debug-mask';
      var card=document.createElement('div'); card.className='dshwv-debug-card';
      var title=document.createElement('div'); title.className='dshwv-debug-title'; title.textContent='组件大小调试'; card.appendChild(title);
      var controls={};
      [['char','大肥鱼',0.5,1.8],['bubble','气泡',0.5,1.8],['label','标题',0.5,2],['amount','余额数字',0.5,2],['hint','提示文字',0.5,2],['button','菜单按钮',0.5,2]].forEach(function(x){
        var r=document.createElement('div'); r.className='dshwv-debug-row';
        var l=document.createElement('label'); l.textContent=x[1];
        var range=document.createElement('input'); range.type='range'; range.min=x[2]; range.max=x[3]; range.step=.05; range.value=state[x[0]];
        var num=document.createElement('input'); num.type='number'; num.className='dshwv-debug-num'; num.min=x[2]; num.max=x[3]; num.step=.05; num.value=state[x[0]];
        r.appendChild(l); r.appendChild(range); r.appendChild(num); card.appendChild(r);
        controls[x[0]]={range:range,num:num};
        function set(v){v=Math.max(x[2],Math.min(x[3],Number(v)||1));v=Math.round(v*100)/100;state[x[0]]=v;range.value=v;num.value=v;apply(state);save(state)}
        range.addEventListener('input',function(){set(range.value)}); num.addEventListener('change',function(){set(num.value)});
      });
      var hint=document.createElement('div'); hint.style.cssText='font-size:11px;color:#9fb0d9;text-align:center;margin-top:6px'; hint.textContent='每个部件独立调整，数值保存在本机。'; card.appendChild(hint);
      var bs=document.createElement('div'); bs.className='dshwv-debug-btns';
      var reset=document.createElement('button'); reset.className='dshwv-debug-btn dshwv-debug-reset'; reset.textContent='恢复默认';
      var close=document.createElement('button'); close.className='dshwv-debug-btn dshwv-debug-ok'; close.textContent='完成';
      bs.appendChild(reset); bs.appendChild(close); card.appendChild(bs); mask.appendChild(card); document.body.appendChild(mask);
      reset.addEventListener('click',function(){state=Object.assign({},defaults);apply(state);save(state);Object.keys(controls).forEach(function(k){controls[k].range.value=state[k];controls[k].num.value=state[k]})});
      close.addEventListener('click',function(){if(mask.parentNode)mask.parentNode.removeChild(mask);syncInteractive()});
      mask.addEventListener('click',function(e){if(e.target===mask)close.click()});
    }
  }
  wait();
})();

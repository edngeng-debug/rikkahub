(function(){
  if(window.__dshwDebugUi)return;
  window.__dshwDebugUi=true;

  function setInteractive(){try{if(window.RikkaDaFeiYu)window.RikkaDaFeiYu.setInteractive(true)}catch(e){}}

  function forceSecondaryUi(){
    if(document.getElementById('dshwv-debug-compat'))return;
    var css=document.createElement('style');css.id='dshwv-debug-compat';
    css.textContent=[
      '.dshwv-bubmask,.dshwv-bubitemmask,.dshwv-qedit,.dshwv-usage-mask,.dshwv-resmask,.dshwv-snapmask{position:fixed!important;left:0!important;top:0!important;right:0!important;bottom:0!important;width:100vw!important;height:100vh!important;min-width:100vw!important;min-height:100vh!important;box-sizing:border-box!important;z-index:30000!important}',
      '.dshwv-bubmask{display:flex!important;align-items:center!important;justify-content:center!important;padding:12px!important;overflow:auto!important}',
      '.dshwv-bubcard{display:block!important;position:relative!important;width:min(440px,calc(100vw - 24px))!important;max-width:calc(100vw - 24px)!important;height:auto!important;max-height:88vh!important;min-height:120px!important;overflow-y:auto!important;overflow-x:hidden!important;flex:none!important;box-sizing:border-box!important}',
      '.dshwv-bubcard>*{box-sizing:border-box!important}',
      '.dshwv-bubrow{display:flex!important;min-height:52px!important;height:auto!important}',
      '.dshwv-bubchip{display:flex!important;min-height:44px!important;height:44px!important}',
      '.dshwv-bubsec{display:block!important;height:auto!important;min-height:18px!important}',
      '.dshwv-bubbtns{display:flex!important;min-height:36px!important}',
      '.dshwv-bubitemcard,.dshwv-qeditcard,.dshwv-usage-card,.dshwv-rescard,.dshwv-snapcard{display:block!important;height:auto!important;max-height:88vh!important;box-sizing:border-box!important}'
    ].join('');
    document.head.appendChild(css);
  }

  function wait(){
    var menu=document.querySelector('.dshwv-menuview'),root=document.querySelector('.dshwv-root');
    if(!menu||!root){setTimeout(wait,250);return}
    forceSecondaryUi();inject(menu,root);setInteractive();
  }

  function inject(menu,root){
    if(document.querySelector('.dshwv-debug-open'))return;
    var style=document.createElement('style');style.id='dshwv-debug-style';
    style.textContent='.dshwv-debug-open{flex:1;border:1px solid rgba(32,49,112,.4);border-radius:6px;background:rgba(32,49,112,.08);color:#203170;font-size:12px;padding:4px 8px;cursor:pointer}.dshwv-debug-mask{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;background:rgba(15,23,42,.55);z-index:31000;display:flex;align-items:center;justify-content:center;padding:12px;box-sizing:border-box}.dshwv-debug-card{width:min(390px,92vw);max-width:92vw;max-height:86vh;overflow:auto;background:#fff;border-radius:12px;padding:16px;box-shadow:0 10px 30px rgba(0,0,0,.3);color:#203170;box-sizing:border-box}.dshwv-debug-title{font-size:15px;font-weight:700;text-align:center;margin-bottom:12px}.dshwv-debug-row{display:flex;align-items:center;gap:8px;margin:9px 0;font-size:12px}.dshwv-debug-row label{width:72px;flex:0 0 72px}.dshwv-debug-row input[type=range]{flex:1;min-width:0}.dshwv-debug-num{width:58px;box-sizing:border-box;border:1px solid rgba(32,49,112,.4);border-radius:6px;padding:3px 5px;color:#203170;background:#fff}.dshwv-debug-btns{display:flex;gap:10px;justify-content:center;margin-top:14px}.dshwv-debug-btn{border:0;border-radius:6px;padding:7px 18px;font-size:13px;cursor:pointer}.dshwv-debug-ok{background:#203170;color:#fff}.dshwv-debug-reset{background:rgba(32,49,112,.1);color:#203170}';
    document.head.appendChild(style);

    var row=document.createElement('div');row.className='dshwv-menu-row';var lab=document.createElement('span');lab.textContent='组件大小';var btn=document.createElement('button');btn.className='dshwv-debug-open';btn.textContent='调试';btn.type='button';row.appendChild(lab);row.appendChild(btn);
    var rows=menu.querySelectorAll('.dshwv-menu-row'),anchor=rows.length?rows[rows.length-1]:null;if(anchor&&anchor.parentNode)anchor.parentNode.insertBefore(row,anchor.nextSibling);else menu.appendChild(row);

    var key='dshw-debug-size-v3',defaults={char:1,bubble:1,label:1,amount:1,hint:1,button:1};
    function load(){try{return Object.assign({},defaults,JSON.parse(localStorage.getItem(key)||'{}'))}catch(e){return Object.assign({},defaults)}}
    function save(s){try{localStorage.setItem(key,JSON.stringify(s))}catch(e){}}
    function apply(s){
      var img=root.querySelector('.dshwv-img');if(img){img.style.setProperty('width',(59.45*s.char)+'%','important');img.style.setProperty('height',(59.45*s.char)+'%','important')}
      var pop=root.querySelector('.dshwv-pop');if(pop){pop.style.setProperty('width',(100*s.bubble)+'%','important');pop.style.setProperty('height','auto','important');pop.style.setProperty('aspect-ratio','1026/700','important');pop.style.setProperty('transform','none','important')}
      var label=root.querySelector('.dshwv-label'),amount=root.querySelector('.dshwv-amount'),hint=root.querySelector('.dshwv-hint');
      if(label)label.style.setProperty('font-size','calc(var(--dshw-u) * '+(66*s.label)+')','important');
      if(amount)amount.style.setProperty('font-size','calc(var(--dshw-u) * '+(128*s.amount)+')','important');
      if(hint)hint.style.setProperty('font-size','calc(var(--dshw-u) * '+(56*s.hint)+')','important');
      var mb=root.querySelector('.dshwv-menu-btn');if(mb){mb.style.setProperty('width',(26*s.button)+'px','important');mb.style.setProperty('height',(26*s.button)+'px','important');mb.style.setProperty('gap',(4*s.button)+'px','important')}
    }
    var current=load();apply(current);btn.addEventListener('click',function(e){e.stopPropagation();open(Object.assign({},current))});
    function open(state){
      var mask=document.createElement('div');mask.className='dshwv-debug-mask';var card=document.createElement('div');card.className='dshwv-debug-card';var title=document.createElement('div');title.className='dshwv-debug-title';title.textContent='组件大小调试';card.appendChild(title);var controls={};
      [['char','大肥鱼',0.5,1.8],['bubble','气泡',0.5,1.8],['label','标题',0.5,2],['amount','余额数字',0.5,2],['hint','提示文字',0.5,2],['button','菜单按钮',0.5,2]].forEach(function(x){
        var r=document.createElement('div');r.className='dshwv-debug-row';var l=document.createElement('label');l.textContent=x[1];var range=document.createElement('input');range.type='range';range.min=x[2];range.max=x[3];range.step=.05;range.value=state[x[0]];var num=document.createElement('input');num.type='number';num.className='dshwv-debug-num';num.min=x[2];num.max=x[3];num.step=.05;num.value=state[x[0]];r.appendChild(l);r.appendChild(range);r.appendChild(num);card.appendChild(r);controls[x[0]]={range:range,num:num};
        function set(v){v=Math.max(x[2],Math.min(x[3],Number(v)||1));v=Math.round(v*100)/100;state[x[0]]=v;range.value=v;num.value=v;current=Object.assign({},state);apply(state);save(state)}
        range.addEventListener('input',function(){set(range.value)});num.addEventListener('change',function(){set(num.value)});
      });
      var h=document.createElement('div');h.style.cssText='font-size:11px;color:#9fb0d9;text-align:center;margin-top:6px';h.textContent='每个部件独立调整，修改立即生效并保存在本机。';card.appendChild(h);var bs=document.createElement('div');bs.className='dshwv-debug-btns';var reset=document.createElement('button');reset.className='dshwv-debug-btn dshwv-debug-reset';reset.textContent='恢复默认';var close=document.createElement('button');close.className='dshwv-debug-btn dshwv-debug-ok';close.textContent='完成';bs.appendChild(reset);bs.appendChild(close);card.appendChild(bs);mask.appendChild(card);document.body.appendChild(mask);
      reset.addEventListener('click',function(){state=Object.assign({},defaults);current=Object.assign({},state);apply(state);save(state);Object.keys(controls).forEach(function(k){controls[k].range.value=state[k];controls[k].num.value=state[k]})});close.addEventListener('click',function(){if(mask.parentNode)mask.parentNode.removeChild(mask)});mask.addEventListener('click',function(e){if(e.target===mask)close.click()});
    }
  }
  wait();
})();

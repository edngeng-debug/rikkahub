(function(){
  if (window.__dshwDebugUi) return; window.__dshwDebugUi=true;
  function wait(){
    var menu=document.querySelector('.dshwv-menuview');
    var root=document.querySelector('.dshwv-root');
    if(!menu||!root){setTimeout(wait,300);return}
    inject(menu,root);
    observeMenu();
  }
  function observeMenu(){
    try{
      var menu=document.querySelector('.dshwv-menu');
      if(!menu) return;
      var sync=function(){try{if(window.RikkaDaFeiYu) window.RikkaDaFeiYu.setMenuOpen(menu.classList.contains('dshwv-menu-open'))}catch(e){}};
      new MutationObserver(sync).observe(menu,{attributes:true,attributeFilter:['class']});
      sync();
    }catch(e){}
  }
  function inject(menu,root){
    if(document.querySelector('.dshwv-debug-open')) return;
    var style=document.createElement('style');
    style.textContent='.dshwv-debug-open{flex:1;border:1px solid rgba(32,49,112,.4);border-radius:6px;background:rgba(32,49,112,.08);color:#203170;font-size:12px;padding:4px 8px;cursor:pointer}.dshwv-debug-open:hover{background:rgba(32,49,112,.16)}.dshwv-debug-mask{position:fixed;inset:0;background:rgba(15,23,42,.55);z-index:31000;display:flex;align-items:center;justify-content:center;color-scheme:light}.dshwv-debug-card{width:min(390px,92vw);max-height:86vh;overflow:auto;background:#fff;border-radius:12px;padding:16px;box-shadow:0 10px 30px rgba(0,0,0,.3);color:#203170}.dshwv-debug-title{font-size:15px;font-weight:700;text-align:center;margin-bottom:12px}.dshwv-debug-row{display:flex;align-items:center;gap:8px;margin:9px 0;font-size:12px}.dshwv-debug-row label{width:72px;flex:0 0 72px}.dshwv-debug-row input[type=range]{flex:1}.dshwv-debug-num{width:58px;box-sizing:border-box;border:1px solid rgba(32,49,112,.4);border-radius:6px;padding:3px 5px;color:#203170;background:#fff}.dshwv-debug-btns{display:flex;gap:10px;justify-content:center;margin-top:14px}.dshwv-debug-btn{border:0;border-radius:6px;padding:7px 18px;font-size:13px;cursor:pointer}.dshwv-debug-ok{background:#203170;color:#fff}.dshwv-debug-reset{background:rgba(32,49,112,.1);color:#203170}';
    document.head.appendChild(style);
    var row=document.createElement('div'); row.className='dshwv-menu-row';
    var lab=document.createElement('span'); lab.textContent='组件大小';
    var btn=document.createElement('button'); btn.className='dshwv-debug-open'; btn.textContent='调试'; btn.type='button';
    row.appendChild(lab); row.appendChild(btn);
    var rows=menu.querySelectorAll('.dshwv-menu-row');
    var anchor=rows.length?rows[rows.length-1]:null;
    if(anchor&&anchor.parentNode) anchor.parentNode.insertBefore(row,anchor.nextSibling); else menu.appendChild(row);
    var key='dshw-debug-size-v1';
    var defaults={char:1,bubble:1,text:1,button:1};
    function load(){try{return Object.assign({},defaults,JSON.parse(localStorage.getItem(key)||'{}'))}catch(e){return Object.assign({},defaults)}}
    function save(s){try{localStorage.setItem(key,JSON.stringify(s))}catch(e){}}
    function apply(s){
      var img=root.querySelector('.dshwv-img'), pop=root.querySelector('.dshwv-pop'), text=root.querySelector('.dshwv-text'), mb=root.querySelector('.dshwv-menu-btn');
      if(img){img.style.width=(59.45*s.char)+'%';img.style.height=(59.45*s.char)+'%'}
      if(pop){pop.style.transform='scale('+s.bubble+')';pop.style.transformOrigin='top left'}
      if(text){text.style.transform='translate(-50%,-50%) scale('+s.text+')'}
      if(mb){mb.style.transform='scale('+s.button+')';mb.style.transformOrigin='top right'}
    }
    var current=load(); apply(current);
    btn.addEventListener('click',function(e){e.stopPropagation();open(current)});
    function open(state){
      var mask=document.createElement('div'); mask.className='dshwv-debug-mask';
      var card=document.createElement('div'); card.className='dshwv-debug-card';
      var title=document.createElement('div'); title.className='dshwv-debug-title'; title.textContent='组件大小调试'; card.appendChild(title);
      var controls={};
      [['char','大肥鱼',0.5,1.8,0.05],['bubble','气泡',0.5,1.8,0.05],['text','字体',0.5,1.8,0.05],['button','菜单按钮',0.5,2,0.05]].forEach(function(x){
        var r=document.createElement('div');r.className='dshwv-debug-row';var l=document.createElement('label');l.textContent=x[1];var range=document.createElement('input');range.type='range';range.min=x[2];range.max=x[3];range.step=x[4];range.value=state[x[0]];var num=document.createElement('input');num.type='number';num.className='dshwv-debug-num';num.min=x[2];num.max=x[3];num.step=x[4];num.value=state[x[0]];r.appendChild(l);r.appendChild(range);r.appendChild(num);card.appendChild(r);controls[x[0]]={range:range,num:num};
        function set(v){v=Math.max(x[2],Math.min(x[3],Number(v)||1));v=Math.round(v*100)/100;state[x[0]]=v;range.value=v;num.value=v;apply(state);save(state)}
        range.addEventListener('input',function(){set(range.value)});num.addEventListener('change',function(){set(num.value)});
      });
      var hint=document.createElement('div');hint.style.cssText='font-size:11px;color:#9fb0d9;text-align:center;margin-top:6px';hint.textContent='调试值会保存在本机，重新打开仍会保留。';card.appendChild(hint);
      var bs=document.createElement('div');bs.className='dshwv-debug-btns';var reset=document.createElement('button');reset.className='dshwv-debug-btn dshwv-debug-reset';reset.textContent='恢复默认';var close=document.createElement('button');close.className='dshwv-debug-btn dshwv-debug-ok';close.textContent='完成';bs.appendChild(reset);bs.appendChild(close);card.appendChild(bs);mask.appendChild(card);document.body.appendChild(mask);
      reset.addEventListener('click',function(){state=Object.assign({},defaults);apply(state);save(state);Object.keys(controls).forEach(function(k){controls[k].range.value=state[k];controls[k].num.value=state[k]})});
      close.addEventListener('click',function(){if(mask.parentNode)mask.parentNode.removeChild(mask)});mask.addEventListener('click',function(e){if(e.target===mask)close.click()});
    }
  }
  wait();
})();

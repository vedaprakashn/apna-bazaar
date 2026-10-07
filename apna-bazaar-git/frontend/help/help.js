const community = document.querySelector('#community');
const search = document.querySelector('#search');
const category = document.querySelector('#category');
const directory = document.querySelector('#directory');
const status = document.querySelector('#status');
const tabs = [...document.querySelectorAll('[role=tab]')];
let contacts = [], section = 'urgent', controller;
const params = new URLSearchParams(location.search);
const selected = params.get('community') || sessionStorage.getItem('heyhood-community');
if (['tridasa','sayuk'].includes(selected)) community.value = selected;
function el(tag,cls,text) { const n=document.createElement(tag); n.className=cls||''; if(text!=null)n.textContent=text; return n; }
function render() {
 directory.replaceChildren();
 const q=search.value.normalize('NFKC').toLowerCase().trim();
 const results=contacts.filter(c=>c.section===section && (!category.value||c.category===category.value) && (!q||[c.name,c.category,c.service_area,c.location,c.notes].join(' ').normalize('NFKC').toLowerCase().includes(q)));
 status.textContent=results.length?`${results.length} ${results.length===1?'contact':'contacts'} · ${community.selectedOptions[0].textContent}`:'No contacts match. Try another service or clear your search.';
 for(const c of results) {
  const card=el('article','contact-card');
  card.append(el('span','contact-category',c.category),el('h2','',c.name));
  const demo=c.is_demo===true;
  card.append(el('span',demo?'verification demo':'verification',demo?'Demo · Unverified':c.verified_at&&c.consent_to_listing?'Verified listing':'Unverified listing'));
  card.append(el('p','contact-area',`${c.scope==='community'?'In your community':'Nearby'} · ${c.service_area}`));
  card.append(el('p','',c.location),el('p','availability',c.availability),el('p','contact-notes',c.notes));
  if(!demo&&c.verified_at&&c.consent_to_listing&&/^\+?[0-9 ()-]{7,25}$/.test(c.phone||'')) {
   const call=el('a','contact-call',`Call ${c.phone} ↗`);call.href=`tel:${c.phone.replace(/[^+0-9]/g,'')}`;card.append(call);
   card.append(el('small','',`Verified ${new Date(c.verified_at).toLocaleDateString('en-IN')}`));
  } else card.append(el('div','demo-phone',demo?`${c.phone||'Example number'} · Demo only`:'Contact awaiting verification'));
  directory.append(card);
 }
}
function categories() {category.replaceChildren(new Option('All services',''));for(const c of [...new Set(contacts.filter(c=>c.section===section).map(c=>c.category))].sort())category.append(new Option(c,c));}
function selectSection(index,focus=false) {
 section=index===0?'urgent':'professional';tabs.forEach((t,i)=>{t.setAttribute('aria-selected',String(i===index));t.tabIndex=i===index?0:-1;});
 directory.setAttribute('aria-labelledby',tabs[index].id);
 document.querySelector('#section-note').textContent=index===0?'Local first-aid and response contacts. Demo listings cannot provide emergency assistance.':'People who can help with everyday care, recovery and legal needs. This is a directory, not a booking service.';
 categories();render();if(focus)tabs[index].focus();
}
tabs.forEach((t,i)=>{t.onclick=()=>selectSection(i);t.onkeydown=e=>{if(['ArrowRight','ArrowLeft','Home','End'].includes(e.key)){e.preventDefault();selectSection(e.key==='Home'?0:e.key==='End'?1:1-i,true);}};});
search.oninput=render;category.onchange=render;
async function load() {
 controller?.abort();controller=new AbortController();contacts=[];directory.replaceChildren();status.textContent='Loading contacts…';
 const slug=community.value;
 history.replaceState(null,'',`?community=${encodeURIComponent(slug)}`);
 try {
  const r=await fetch(`/api/${encodeURIComponent(slug)}/help-contacts`,{signal:controller.signal});if(!r.ok)throw Error();
  const data=await r.json();if(slug!==community.value)return;contacts=data.contacts||[];categories();render();
 }catch(e){if(e.name==='AbortError')return;status.textContent='The local directory couldn’t load. You can still call 112 above in a real emergency.';}
}
community.onchange=load;load();

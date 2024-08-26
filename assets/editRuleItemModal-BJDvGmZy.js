import{p as M,U as V,A as N}from"./index-CRN7nCaW.js";import{R as I,f as x,a6 as D,r as w,a as L,j as P,s as T,t as A,y as _,v as q,x as z,a2 as h}from"./libs-Bgy1OBX3.js";import{M as k,o as B,F as E,av as F,p as J,q as $}from"./arcoDesign-BkyBQC66.js";function v(u){if(typeof u!="string")throw new TypeError("Path must be a string. Received "+JSON.stringify(u))}function y(u,e){for(var r="",a=0,t=-1,n=0,l,s=0;s<=u.length;++s){if(s<u.length)l=u.charCodeAt(s);else{if(l===47)break;l=47}if(l===47){if(!(t===s-1||n===1))if(t!==s-1&&n===2){if(r.length<2||a!==2||r.charCodeAt(r.length-1)!==46||r.charCodeAt(r.length-2)!==46){if(r.length>2){var o=r.lastIndexOf("/");if(o!==r.length-1){o===-1?(r="",a=0):(r=r.slice(0,o),a=r.length-1-r.lastIndexOf("/")),t=s,n=0;continue}}else if(r.length===2||r.length===1){r="",a=0,t=s,n=0;continue}}e&&(r.length>0?r+="/..":r="..",a=2)}else r.length>0?r+="/"+u.slice(t+1,s):r=u.slice(t+1,s),a=s-t-1;t=s,n=0}else l===46&&n!==-1?++n:n=-1}return r}function j(u,e){var r=e.dir||e.root,a=e.base||(e.name||"")+(e.ext||"");return r?r===e.root?r+a:r+u+a:a}var C={resolve:function(){for(var e="",r=!1,a,t=arguments.length-1;t>=-1&&!r;t--){var n;t>=0?n=arguments[t]:(a===void 0&&(a=M.cwd()),n=a),v(n),n.length!==0&&(e=n+"/"+e,r=n.charCodeAt(0)===47)}return e=y(e,!r),r?e.length>0?"/"+e:"/":e.length>0?e:"."},normalize:function(e){if(v(e),e.length===0)return".";var r=e.charCodeAt(0)===47,a=e.charCodeAt(e.length-1)===47;return e=y(e,!r),e.length===0&&!r&&(e="."),e.length>0&&a&&(e+="/"),r?"/"+e:e},isAbsolute:function(e){return v(e),e.length>0&&e.charCodeAt(0)===47},join:function(){if(arguments.length===0)return".";for(var e,r=0;r<arguments.length;++r){var a=arguments[r];v(a),a.length>0&&(e===void 0?e=a:e+="/"+a)}return e===void 0?".":C.normalize(e)},relative:function(e,r){if(v(e),v(r),e===r||(e=C.resolve(e),r=C.resolve(r),e===r))return"";for(var a=1;a<e.length&&e.charCodeAt(a)===47;++a);for(var t=e.length,n=t-a,l=1;l<r.length&&r.charCodeAt(l)===47;++l);for(var s=r.length,o=s-l,m=n<o?n:o,c=-1,i=0;i<=m;++i){if(i===m){if(o>m){if(r.charCodeAt(l+i)===47)return r.slice(l+i+1);if(i===0)return r.slice(l+i)}else n>m&&(e.charCodeAt(a+i)===47?c=i:i===0&&(c=0));break}var f=e.charCodeAt(a+i),d=r.charCodeAt(l+i);if(f!==d)break;f===47&&(c=i)}var g="";for(i=a+c+1;i<=t;++i)(i===t||e.charCodeAt(i)===47)&&(g.length===0?g+="..":g+="/..");return g.length>0?g+r.slice(l+c):(l+=c,r.charCodeAt(l)===47&&++l,r.slice(l))},_makeLong:function(e){return e},dirname:function(e){if(v(e),e.length===0)return".";for(var r=e.charCodeAt(0),a=r===47,t=-1,n=!0,l=e.length-1;l>=1;--l)if(r=e.charCodeAt(l),r===47){if(!n){t=l;break}}else n=!1;return t===-1?a?"/":".":a&&t===1?"//":e.slice(0,t)},basename:function(e,r){if(r!==void 0&&typeof r!="string")throw new TypeError('"ext" argument must be a string');v(e);var a=0,t=-1,n=!0,l;if(r!==void 0&&r.length>0&&r.length<=e.length){if(r.length===e.length&&r===e)return"";var s=r.length-1,o=-1;for(l=e.length-1;l>=0;--l){var m=e.charCodeAt(l);if(m===47){if(!n){a=l+1;break}}else o===-1&&(n=!1,o=l+1),s>=0&&(m===r.charCodeAt(s)?--s===-1&&(t=l):(s=-1,t=o))}return a===t?t=o:t===-1&&(t=e.length),e.slice(a,t)}else{for(l=e.length-1;l>=0;--l)if(e.charCodeAt(l)===47){if(!n){a=l+1;break}}else t===-1&&(n=!1,t=l+1);return t===-1?"":e.slice(a,t)}},extname:function(e){v(e);for(var r=-1,a=0,t=-1,n=!0,l=0,s=e.length-1;s>=0;--s){var o=e.charCodeAt(s);if(o===47){if(!n){a=s+1;break}continue}t===-1&&(n=!1,t=s+1),o===46?r===-1?r=s:l!==1&&(l=1):r!==-1&&(l=-1)}return r===-1||t===-1||l===0||l===1&&r===t-1&&r===a+1?"":e.slice(r,t)},format:function(e){if(e===null||typeof e!="object")throw new TypeError('The "pathObject" argument must be of type Object. Received type '+typeof e);return j("/",e)},parse:function(e){v(e);var r={root:"",dir:"",base:"",ext:"",name:""};if(e.length===0)return r;var a=e.charCodeAt(0),t=a===47,n;t?(r.root="/",n=1):n=0;for(var l=-1,s=0,o=-1,m=!0,c=e.length-1,i=0;c>=n;--c){if(a=e.charCodeAt(c),a===47){if(!m){s=c+1;break}continue}o===-1&&(m=!1,o=c+1),a===46?l===-1?l=c:i!==1&&(i=1):l!==-1&&(i=-1)}return l===-1||o===-1||i===0||i===1&&l===o-1&&l===s+1?o!==-1&&(s===0&&t?r.base=r.name=e.slice(1,o):r.base=r.name=e.slice(s,o)):(s===0&&t?(r.name=e.slice(1,l),r.base=e.slice(1,o)):(r.name=e.slice(s,l),r.base=e.slice(s,o)),r.ext=e.slice(l,o)),s>0?r.dir=e.slice(0,s-1):t&&(r.dir="/"),r},sep:"/",delimiter:":",win32:null,posix:null};C.posix=C;var G=C;const H=I(G),W=x({__name:"editRuleItemModal",setup(u,{expose:e}){const{t:r}=D(),a=w(!1),t=w(!1),n=L({ruleId:"",ruleName:"",subUrl:""}),l={ruleId:[{required:!0}],ruleName:[{required:!0,message:r("page.rule_management.ruleSubscribe.editModal.form.name.required")}],subUrl:[{type:"url",required:!0}]};let s;e({showModal:(i,f,d)=>{t.value=i,s=f,!i&&d?(n.ruleId=d.ruleId,n.ruleName=d.ruleName,n.subUrl=d.subUrl):(n.ruleId=Math.random().toString(36).slice(2,10),n.ruleName="",n.subUrl=""),a.value=!0}});const o=w(),m=async()=>{var f;if(await((f=o.value)==null?void 0:f.validate()))return!1;if(t.value){const d=await N(n);return d.success?(s&&s(n),!0):(k.error(d.message),!0)}else{const d=await V(n);return d.success?(s&&s(n),!0):(k.error(d.message),!0)}},c=i=>{if(!n.ruleName)try{const f=new URL(i),d=H.parse(f.pathname).name;n.ruleName=d}catch{}};return(i,f)=>{const d=B,g=E,U=F,S=J,R=$;return P(),T(R,{visible:a.value,"onUpdate:visible":f[3]||(f[3]=b=>a.value=b),title:t.value?h(r)("page.rule_management.ruleSubscribe.editModal.title.new"):h(r)("page.rule_management.ruleSubscribe.editModal.title"),unmountOnClose:"",onBeforeOk:m},{default:A(()=>[_(S,{ref_key:"formRef",ref:o,model:n,rules:l},{default:A(()=>[_(g,{field:"ruleId",label:"ID"},{extra:A(()=>[q(z(h(r)("page.rule_management.ruleSubscribe.editModal.form.id.extra")),1)]),default:A(()=>[_(d,{modelValue:n.ruleId,"onUpdate:modelValue":f[0]||(f[0]=b=>n.ruleId=b),disabled:!t.value,"allow-clear":""},null,8,["modelValue","disabled"])]),_:1}),_(g,{field:"ruleName",label:h(r)("page.rule_management.ruleSubscribe.editModal.form.name")},{default:A(()=>[_(d,{modelValue:n.ruleName,"onUpdate:modelValue":f[1]||(f[1]=b=>n.ruleName=b),"allow-clear":""},null,8,["modelValue"])]),_:1},8,["label"]),_(g,{field:"subUrl",label:"URL"},{default:A(()=>[_(U,{modelValue:n.subUrl,"onUpdate:modelValue":f[2]||(f[2]=b=>n.subUrl=b),"allow-clear":"",onChange:c,"auto-size":{minRows:2,maxRows:5}},null,8,["modelValue"])]),_:1})]),_:1},8,["model"])]),_:1},8,["visible","title"])}}});export{W as default};

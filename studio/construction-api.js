/* GD Hub construction authoring API. Stores drafts; does not call Geometry Dash internals. */
(function(g){"use strict";
function state(s){if(!s||s.format!=="gdhub-build-draft-1"||!Array.isArray(s.objects)||!Array.isArray(s.groups)||!Array.isArray(s.triggers))throw Error("Estado de construção inválido");return s}
function unique(s,id){if(!id||[...s.objects,...s.groups,...s.triggers].some(x=>x.id===id))throw Error("ID ausente ou duplicado: "+id)}
function createObject(s,v){state(s);let id=String(v.id||"");unique(s,id);let o={id,tipo:String(v.tipo||"bloco"),x:Number(v.x)||0,y:Number(v.y)||0,propriedades:{...(v.propriedades||{})}};s.objects.push(o);return o}
function setObjectProperty(s,id,key,value){state(s);let o=s.objects.find(x=>x.id===id);if(!o)throw Error("Objeto não encontrado: "+id);if(!key)throw Error("Propriedade vazia");o.propriedades[key]=value;return o}
function createGroup(s,v){state(s);let id=String(v.id||"");unique(s,id);let group={id,nome:String(v.nome||id),objetos:[]};s.groups.push(group);return group}
function addToGroup(s,groupId,objectId){state(s);let group=s.groups.find(x=>x.id===groupId||x.nome===groupId),obj=s.objects.find(x=>x.id===objectId);if(!group||!obj)throw Error("Grupo ou objeto não encontrado");if(!group.objetos.includes(objectId))group.objetos.push(objectId);obj.propriedades.grupo=group.id;return group}
function createTrigger(s,v){state(s);let id=String(v.id||"");unique(s,id);let t={id,tipo:String(v.tipo||"evento"),x:Number(v.x)||0,y:Number(v.y)||0,alvo:v.alvo||null,config:{...(v.config||{})}};s.triggers.push(t);return t}
function configureTrigger(s,id,key,value){state(s);let t=s.triggers.find(x=>x.id===id);if(!t)throw Error("Trigger não encontrado: "+id);if(!key)throw Error("Configuração vazia");t.config[key]=value;return t}
function remove(s,id){state(s);let before=s.objects.length+s.groups.length+s.triggers.length;for(let k of ["objects","groups","triggers"])s[k]=s[k].filter(x=>x.id!==id);for(let group of s.groups)group.objetos=group.objetos.filter(x=>x!==id);return before!==s.objects.length+s.groups.length+s.triggers.length}
function reset(){return {format:"gdhub-build-draft-1",objects:[],groups:[],triggers:[]}}
function getData(s){return JSON.parse(JSON.stringify(state(s)))}
g.GDHubBuild=Object.freeze({createObject,setObjectProperty,createGroup,addToGroup,createTrigger,configureTrigger,remove,reset,getData});
})(window);
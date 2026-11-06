"""Run after Firebase Auth/RTDB emulators start with project demo-ourlist-audit. No live service writes."""
import json,urllib.request,urllib.error,time,pathlib,secrets
base='http://127.0.0.1:9000';ns='demo-ourlist-audit-default-rtdb';results=[]
authbase='http://127.0.0.1:9099/identitytoolkit.googleapis.com';key='fake'

def http(url,method='GET',data=None,headers=None):
 req=urllib.request.Request(url,data=None if data is None else json.dumps(data).encode(),method=method,headers={'Content-Type':'application/json',**(headers or {})})
 try:
  with urllib.request.urlopen(req,timeout=20) as r:return r.status,json.loads(r.read() or 'null')
 except urllib.error.HTTPError as e:return e.code,json.loads(e.read() or 'null')
def user():
 s,d=http(authbase+'/v1/accounts:signUp?key='+key,'POST',{'returnSecureToken':True});assert s==200,(s,d);return d['localId'],d['idToken']
a,at=user();b,bt=user();c,ct=user();code=''.join(str(secrets.randbelow(10)) for _ in range(6));room='privateRooms/'+code
meta=dict(createdAt=1,deviceA='A',userAName='owner',deviceB='',userBName='partner',datingStartDate='',closed=False,ownerUid=a,partnerUid='',protocolVersion=2)
state=dict(shared=[],heart=[],lastUpdated=1,lastUpdatedBy='A',appliedOperationIds=[])
def call(path,method='GET',data=None,token=None):return http(base+'/'+path+'.json?'+('ns='+ns+'&' if ns else '')+('' if not token else 'auth='+token),method,data)
def check(name,path,method='GET',data=None,token=None,expect=200):
 s,d=call(path,method,data,token);ok=(s==expect or (expect==401 and s==403)) and (expect!=401 or 'permission denied' in str(d).lower());results.append({'test':name,'passed':ok,'status':s,'expected':expect});print(name,s,'PASS' if ok else repr(d));return d
check('unauthenticated root denied','',expect=401)
check('legacy root denied','rooms/123456',token=at,expect=401)
check('public update readable','app_version')
check('public update client write denied','app_version','PUT',{'latestVersionCode':99},at,401)
check('owner creates protected room',room,'PUT',{'meta':meta,'state':state},at)
check('owner reads room',room,token=at)
check('unauthenticated room denied',room,expect=401)
check('stranger reads room denied',room,token=ct,expect=401)
check('stranger overwrites room denied',room,'PUT',{'meta':meta,'state':state},ct,401)
check('unapproved partner state denied',room+'/state',token=bt,expect=401)
request=dict(deviceId='B',userName='partner',confirmationCode='123123',createdAt=1)
check('partner requests approval','pairRequests/'+code+'/'+b,'PUT',request,bt)
check('requester reads own confirmation','pairRequests/'+code+'/'+b,token=bt)
check('stranger reads requests denied','pairRequests/'+code,token=ct,expect=401)
check('requester still cannot read state',room+'/state',token=bt,expect=401)
check('requester cannot self approve',room+'/meta','PUT',{**meta,'partnerUid':b,'deviceB':'B','userBName':'partner'},bt,401)
check('owner cannot assign without request',room+'/meta','PUT',{**meta,'partnerUid':c,'deviceB':'C','userBName':'stranger'},at,401)
meta.update(partnerUid=b,deviceB='B',userBName='partner')
check('owner approves matching request',room+'/meta','PUT',meta,at)
check('approved partner reads state',room+'/state',token=bt)
check('third device request denied','pairRequests/'+code+'/'+c,'PUT',{**request,'deviceId':'C'},ct,401)
check('partner cannot replace membership',room+'/meta','PUT',{**meta,'partnerUid':c},bt,401)
op=dict(opId='op1',collection='SHARED',deviceId='A',itemId='item1',kind='CREATE',createdAt=1,deviceSequence=1)
check('owner appends operation',room+'/operations/op1','PUT',op,at)
check('same operation retry allowed',room+'/operations/op1','PUT',op,at)
check('rewriting operation denied',room+'/operations/op1','PUT',{**op,'kind':'DELETE'},at,401)
check('partner cannot impersonate owner',room+'/operations/op2','PUT',{**op,'opId':'op2'},bt,401)
check('partner appends own operation',room+'/operations/op2','PUT',{**op,'opId':'op2','deviceId':'B'},bt)
check('personal scope op denied',room+'/operations/op3','PUT',{**op,'opId':'op3','collection':'PERSONAL'},at,401)
check('partner state own device allowed',room+'/state','PUT',{**state,'lastUpdatedBy':'B'},bt)
check('state actor impersonation denied',room+'/state','PUT',state,bt,401)
check('private snapshot injection denied',room+'/state','PUT',{**state,'shared':[dict(id='private',collection='SECRET',title='secret',status='TODO')]},at,401)
check('owner closes room',room+'/meta','PUT',{**meta,'closed':True},at)
check('closed room state denied',room+'/state',token=bt,expect=401)
check('closed room write denied',room+'/operations/op4','PUT',{**op,'opId':'op4'},at,401)
check('closed meta readable for disconnect',room+'/meta',token=bt)
p=pathlib.Path('docs/research/2026-11-06-ourlist-fixes/firebase-security-results.json')
p.write_text(json.dumps({'environment':'official Firebase Auth + RTDB emulators, demo project only','results':results},indent=2)+'\n')
assert all(x['passed'] for x in results), 'security test failed'
print(str(len(results))+' security checks passed')

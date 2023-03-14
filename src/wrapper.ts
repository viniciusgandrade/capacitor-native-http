import { Observable } from 'rxjs';

import { HttpNative } from './index';

export const callNative = (req: any): Observable<any> => {
  return new Observable<any>((ob: any) => {
    const headers = {} as any;
    req.headers.keys().forEach((key: string) => {
      headers[key] = req.headers.get(key);
    });
    let data: any = req.body;
    if (req.method === 'POST' && headers['Content-Type'] === 'application/x-www-form-urlencoded' && req.body) {
      data = {};
      const params = req.body.split('&');
      for (const param of params) {
        try {
          data[param.split('=')[0]] = decodeURIComponent(param.split('=')[1]);
        } catch {
          data[param.split('=')[0]] = param.split('=')[1];
        }
      }
    }
    const params: any = {};
    req.params.keys().forEach((key: string) => {
      params[key] = req.params.get(key);
    });
    HttpNative.request({
      method: req.method,
      data,
      params,
      headers,
      url: req.url
    }).then((res: any) => {
      ob.next({
        body: JSON.parse(res.data),
        headers: res.headers ? JSON.parse(res.headers) : {}
      });
      ob.complete();
    }).catch((error: any) => {
      const obj = checkJson(error.error);
      if (typeof obj === 'string') {
        ob.error({ error: obj });
      }
      ob.error(obj);
    })
  });
}

function checkJson(error: string) {
  try {
    return JSON.parse(error);
  } catch (err) {
    return error;
  }
}

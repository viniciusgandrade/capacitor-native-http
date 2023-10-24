import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { HttpNative } from './index';

export const callNative = (req: any): Observable<any> => {
  return new Observable<any>((ob: any) => {
    let reqCopy = req;
    const headers = {} as any;
    reqCopy.headers.keys().forEach((key: string) => {
      headers[key] = reqCopy.headers.get(key);
    });
    let data: any = reqCopy.body;
    if (reqCopy.method === 'POST' && headers['Content-Type'] === 'application/x-www-form-urlencoded' && reqCopy.body) {
      data = {};
      const params = reqCopy.body.split('&');
      for (const param of params) {
        try {
          data[param.split('=')[0]] = decodeURIComponent(param.split('=')[1]);
        } catch {
          data[param.split('=')[0]] = param.split('=')[1];
        }
      }
    } else if (reqCopy.method === 'GET' && reqCopy.url.includes('?')) {
      let params: any = reqCopy.url
        .split('?')[1]
        ?.split('&')
        ?.map((p: { split: (arg0: string) => [any, any]; }) => {
          const [key, value] = p.split('=');
          return { [key]: value };
        })
        .reduce((acc: { [x: string]: any; }, curr: { [x: string]: any; }) => {
          let key: keyof typeof curr;
          // eslint-disable-next-line guard-for-in
          for (key in curr) {
            acc[key] = curr[key];
          }
          return acc;
        });
      params = new HttpParams({ fromObject: params });
      reqCopy = reqCopy.clone({
        params,
        url: reqCopy.url.split('?')[0],
      });
    }
    const params: any = {};
    reqCopy.params.keys().forEach((key: string) => {
      params[key] = reqCopy.params.get(key);
    });
    HttpNative.request({
      method: reqCopy.method,
      data,
      params,
      headers,
      url: reqCopy.url
    }).then((res: any) => {
      ob.next({
        body: checkJson(res.data),
        headers: res.headers ? JSON.parse(res.headers) : {}
      });
      ob.complete();
    }).catch((error: any) => {
      console.log('erro request!');
      ob.error(checkJson(error.message || error.error || error.errorMessage));
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

import { Observable, Subscriber } from 'rxjs';

import { HttpNative } from './index';

export const callNative = (req: any): Observable<any> => {
  return new Observable(ob => {
    const headers: any = {};
    req.headers.keys().forEach((key: any) => {
      headers[key] = req.headers.get(key);
    });
    console.log('montou headers');
    let data = req.body;

    if (
      req.method === 'POST' &&
      headers['Content-Type'] === 'application/x-www-form-urlencoded' &&
      req.body
    ) {
      data = {};
      const params = req.body.split('&');
      for (const param of params) {
        try {
          data[param.split('=')[0]] = decodeURIComponent(param.split('=')[1]);
        } catch (_a) {
          data[param.split('=')[0]] = param.split('=')[1];
        }
      }
    }

    const params: any = {};
    req.params.keys().forEach((key: any) => {
      params[key] = req.params.get(key);
    });
    if (headers['Content-Type'].includes('multipart/form-data')) {
      convertFormData(req.body).then(formData => {
        makeRequest({
          method: req.method,
          data: formData.data,
          params,
          headers,
          url: req.url,
          ob: ob,
        });
      });
    } else {
      makeRequest({
        method: req.method,
        data,
        params,
        headers,
        url: req.url,
        ob: ob,
      });
    }
  });
};

function makeRequest(req: {
  method: string;
  data: any;
  params: any;
  headers: any;
  url: string;
  ob: Subscriber<any>;
}) {
  HttpNative.request({
    method: req.method,
    data: req.data,
    params: req.params,
    headers: req.headers,
    url: req.url,
  })
    .then((res: any) => {
      let responseBody;
      const headers = JSON.parse(res.headers);
      const contentType = headers['Content-Type'];
      if (contentType.includes('application/json')) {
        responseBody = checkJson(res.data);
      } else if (contentType.includes('text/')) {
        responseBody = res.data;
      } else {
        responseBody = base64ToBlob(res.data, contentType);
      }
      req.ob.next({
        body: responseBody,
        headers,
      });
      req.ob.complete();
    })
    .catch(error => {
      console.log('erro request!');
      req.ob.error(checkJson(error.message || error.error || error.errorMessage));
    });
}

function base64ToBlob(base64String: string, contentType = '') {
  const byteCharacters = atob(base64String);
  const byteArrays = [];

  for (let i = 0; i < byteCharacters.length; i++) {
    byteArrays.push(byteCharacters.charCodeAt(i));
  }
  const byteArray = new Uint8Array(byteArrays);
  return new Blob([byteArray], { type: contentType });
}

const readFileAsBase64 = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const data = reader.result as string;
      resolve(btoa(data));
    };
    reader.onerror = reject;

    reader.readAsBinaryString(file);
  });

const convertFormData = async (formData: FormData): Promise<any> => {
  const newFormData: any[] = [];
  for (const pair of formData.entries()) {
    const [key, value] = pair;
    if (value instanceof File) {
      const base64File = await readFileAsBase64(value);
      newFormData.push({
        key,
        value: base64File,
        type: 'base64File',
        contentType: value.type,
        fileName: value.name,
      });
    } else {
      newFormData.push({ key, value, type: 'string' });
    }
  }

  return {
    data: formData,
    type: 'formData',
  };
};

function checkJson(error: string) {
  try {
    return JSON.parse(error);
  } catch (err) {
    return error;
  }
}

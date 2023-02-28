import { registerPlugin } from '@capacitor/core';

import type { HttpNativePlugin } from './definitions';

const HttpNative = registerPlugin<HttpNativePlugin>('HttpNative', {
  web: () => import('./web').then(m => new m.HttpNativeWeb()),
});

export * from './definitions';
export { HttpNative };

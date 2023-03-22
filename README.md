# capacitor-native-http

Plugin capacitor para requisições nativas

## Install

```bash
npm install capacitor-native-http
npx cap sync
```

## API

<docgen-index>

* [`request(...)`](#request)
* [`initialize(...)`](#initialize)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### request(...)

```typescript
request(options: { method: string; url: string; params?: any; data?: any; headers?: any; }) => Promise<{ data: any; }>
```

| Param         | Type                                                                                   |
| ------------- | -------------------------------------------------------------------------------------- |
| **`options`** | <code>{ method: string; url: string; params?: any; data?: any; headers?: any; }</code> |

**Returns:** <code>Promise&lt;{ data: any; }&gt;</code>

--------------------


### initialize(...)

```typescript
initialize(options: { hostname?: string[]; certPath?: string; timeout?: number; }) => Promise<{ data: any; }>
```

| Param         | Type                                                                       |
| ------------- | -------------------------------------------------------------------------- |
| **`options`** | <code>{ hostname?: string[]; certPath?: string; timeout?: number; }</code> |

**Returns:** <code>Promise&lt;{ data: any; }&gt;</code>

--------------------

</docgen-api>

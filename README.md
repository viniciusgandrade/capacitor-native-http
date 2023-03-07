# capacitor-native-http

Plugin capacitor para requisições nativas

## Install

```bash
npm install capacitor-native-http
npx cap sync
```

## API

<docgen-index>

* [`doPost(...)`](#dopost)
* [`doGet(...)`](#doget)
* [`initialize(...)`](#initialize)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### doPost(...)

```typescript
doPost(options: { url: string; data: any; headers: any; }) => Promise<{ data: any; }>
```

| Param         | Type                                                   |
| ------------- | ------------------------------------------------------ |
| **`options`** | <code>{ url: string; data: any; headers: any; }</code> |

**Returns:** <code>Promise&lt;{ data: any; }&gt;</code>

--------------------


### doGet(...)

```typescript
doGet(options: { url: string; params: any; headers: any; }) => Promise<{ data: any; }>
```

| Param         | Type                                                     |
| ------------- | -------------------------------------------------------- |
| **`options`** | <code>{ url: string; params: any; headers: any; }</code> |

**Returns:** <code>Promise&lt;{ data: any; }&gt;</code>

--------------------


### initialize(...)

```typescript
initialize(options: { hostname: string; certPath: string; }) => Promise<{ data: any; }>
```

| Param         | Type                                                 |
| ------------- | ---------------------------------------------------- |
| **`options`** | <code>{ hostname: string; certPath: string; }</code> |

**Returns:** <code>Promise&lt;{ data: any; }&gt;</code>

--------------------

</docgen-api>

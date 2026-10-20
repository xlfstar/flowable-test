/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

declare module 'bpmn-js/lib/Modeler' {
  const Modeler: any
  export default Modeler
}

declare module 'bpmn-js/lib/NavigatedViewer' {
  const NavigatedViewer: any
  export default NavigatedViewer
}

declare module 'bpmn-js/lib/Viewer' {
  const Viewer: any
  export default Viewer
}

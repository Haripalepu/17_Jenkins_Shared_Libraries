
#!groovy

def decidePipeline(Map configMap){
    application = configMap.get("application")
    switch(application) {  //If the application is nodejsvm then take that code if java take another code.
        case 'nodejsVM':
            nodejsVM(configMap)
            break
        case 'javaVM':
            javaVM(configMap)
            break
        case 'nodejsEKS':
            nodejsEKS(configMap)
            break
        default:
            error "Application is not recognised"
            break
    }
}
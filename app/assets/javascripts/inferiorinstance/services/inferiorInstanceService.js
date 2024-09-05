define([],function() {
    'use strict';

    function InferiorInstanceService(playRoutes) {

        this.getInferiorInstances = function () {
            console.log('GET INFERIOR INSTANCES');
            return $http.get('/instances/inferior');
            //return playRoutes.controllers.Interconnections.getInferiorInstances().get();
        };
        this.getInferiorInstancesStatus = function () {
            console.log('GET INFERIOR INSTANCES STATUS');
            return $http.get('/instances/inferior/status');
            //return playRoutes.controllers.Interconnections.getInferiorInstancesStatus().get();
        };
        this.updateInferiorInstance = function (inferiorInstance) {
            console.log('UPDATE INFERIOR INSTANCE');
            return $http.put('/instances/inferior');
            //return playRoutes.controllers.Interconnections.updateInferiorInstance().put(inferiorInstance);
        };
        this.getConnectionStatus = function (url) {
            console.log('GET CONNECTION STATUS');
            return $http.get('/connections/status', {params: { url: url }});
            //return playRoutes.controllers.Interconnections.getConnectionStatus(url).get();
        };
    }

    return InferiorInstanceService;

});
define([],function() {
    'use strict';

    function InferiorInstanceService(playRoutes) {

        this.getInferiorInstances = function () {
            return playRoutes.controllers.Interconnections.getInferiorInstances().get();
        };
        this.getInferiorInstancesStatus = function () {
            return playRoutes.controllers.Interconnections.getInferiorInstancesStatus().get();
        };
        this.updateInferiorInstance = function (inferiorInstance) {
            return playRoutes.controllers.Interconnections.updateInferiorInstance().put(inferiorInstance);
        };
        this.getConnectionStatus = function (url) {
            return playRoutes.controllers.Interconnections.getConnectionStatus(url).get();
        };
    }

    return InferiorInstanceService;

});
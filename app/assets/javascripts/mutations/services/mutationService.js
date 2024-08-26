define(['lodash'],function(_) {
    'use strict';

    function MutationService(playRoutes,$q) {

        this.getMutationModelsTypes = function () {
            return playRoutes.controllers.MutationController.getMutationModelsTypes().get();
        };
        this.getMutationModels = function()  {
            return playRoutes.controllers.MutationController.getAllMutationModels().get();
        };
        this.getMutationDefaultParams = function()  {
            return playRoutes.controllers.MutationController.getAllMutationDefaultParameters().get();
        };
        this.getActiveMutationModels = function()  {
            return playRoutes.controllers.MutationController.getActiveMutationModels().get();
        };
        this.updateMutationModel = function(mutationModel)  {
            return playRoutes.controllers.MutationController.update().put(mutationModel);
        };
        this.generateMatrix = function(mutationModel)  {
            mutationModel.parameters = [];
            return playRoutes.controllers.MutationController.generateMatrix().put(mutationModel);
        };
        this.updateMutationModelByChunks = function(mutationModel)  {
            var requestUpdate = {};
            requestUpdate.header = mutationModel.header;
            var parameterChunk = _.chunk(mutationModel.parameters, 5);
            var promises = parameterChunk.map(function (element) {
                var req = _.cloneDeep(requestUpdate);
                req.parameters = element;
                return playRoutes.controllers.MutationController.update().put(req);
            });
            requestUpdate.parameters = [];
            return $q.all(promises);
        };
        this.getMutationModel = function(mutationModel)  {
            return playRoutes.controllers.MutationController.getMutationModel(mutationModel).get();
        };
        this.getMutatitionModelParameters = function(mutationModel)  {
            return playRoutes.controllers.MutationController.getMutatitionModelParameters(mutationModel).get();
        };
        this.insertMutationModel = function(mutationModel)  {
            return playRoutes.controllers.MutationController.insert().post(mutationModel);
        };
        this.deleteMutationModelById = function(id)  {
            return playRoutes.controllers.MutationController.deleteMutationModelById(id).delete();
        };

    }

    return MutationService;

});
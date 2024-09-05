define(['lodash'],function(_) {
    'use strict';

    function MutationService(playRoutes,$q, $http) {

        this.getMutationModelsTypes = function () {
            return $http.get('/mutation-models-types');
            //return playRoutes.controllers.MutationController.getMutationModelsTypes().get();
        };
        this.getMutationModels = function()  {
            return $http.get('/mutation-models');
            //return playRoutes.controllers.MutationController.getAllMutationModels().get();
        };
        this.getMutationDefaultParams = function()  {
            console.log('GET MUTATION DEFAULT PARAMS');
            return $http.get('/mutation-models/default-params');
            //return playRoutes.controllers.MutationController.getAllMutationDefaultParameters().get();
        };
        this.getActiveMutationModels = function()  {
            console.log('GET ACTIVE MUTATION MODELS');
            return $http.get('/mutation-models/active');
            //return playRoutes.controllers.MutationController.getActiveMutationModels().get();
        };
        this.updateMutationModel = function(mutationModel)  {
            console.log('UPDATE MUTATION MODEL');
            return $http.put('/mutation-models', mutationModel);
            //return playRoutes.controllers.MutationController.update().put(mutationModel);
        };
        this.generateMatrix = function(mutationModel)  {
            mutationModel.parameters = [];
            console.log('GENERATE MATRIX');
            return $http.put('/mutation-models-matrix', mutationModel);
            //return playRoutes.controllers.MutationController.generateMatrix().put(mutationModel);
        };
        this.updateMutationModelByChunks = function(mutationModel)  {
            var requestUpdate = {};
            requestUpdate.header = mutationModel.header;
            var parameterChunk = _.chunk(mutationModel.parameters, 5);
            var promises = parameterChunk.map(function (element) {
                var req = _.cloneDeep(requestUpdate);
                req.parameters = element;
                return $http.put('/mutation-models', req);
                //return playRoutes.controllers.MutationController.update().put(req);
            });
            requestUpdate.parameters = [];
            return $q.all(promises);
        };
        this.getMutationModel = function(mutationModel)  {
            console.log('GET MUTATION MODEL');
            return $http.get('/mutation-model', { params: { id: mutationModel } });
            //return playRoutes.controllers.MutationController.getMutationModel(mutationModel).get();
        };
        this.getMutatitionModelParameters = function(mutationModel)  {
            console.log('GET MUTATION MODEL PARAMETERS');
            return $http.get('/mutation-model-parameters', { params: { id: mutationModel } });
            //return playRoutes.controllers.MutationController.getMutatitionModelParameters(mutationModel).get();
        };
        this.insertMutationModel = function(mutationModel)  {
            console.log('INSERT MUTATION MODEL');
            return $http.post('/mutation-models', mutationModel);
            //return playRoutes.controllers.MutationController.insert().post(mutationModel);
        };
        this.deleteMutationModelById = function(id)  {
            console.log('DLETE MUTATION MODEL BY ID');
            return $http.delete('/mutation-models/' + id);
            //return playRoutes.controllers.MutationController.deleteMutationModelById(id).delete();
        };

    }

    return MutationService;

});
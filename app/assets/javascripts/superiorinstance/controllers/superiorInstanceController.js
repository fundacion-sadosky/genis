define(['lodash'], function(_) {
    'use strict';

    function SuperiorInstanceController($scope, superiorInstanceService,alertService) {

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.init = function() {
            $scope.changed = false;
            $scope.categories = {};
            $scope.categories.data = {};
            $scope.msgErrorCategorias = "No se pudo obtener las categorías de la instancia superior";
            $scope.isProcessing = true;

            superiorInstanceService.listCategoriesMapping().then(function(response) {
                $scope.categoriesMappings = response.data;
            }, function() {
            });

            superiorInstanceService.getConnections().then(function(response) {
                $scope.connections = response.data;
                $scope.isProcessing = false;
            }, function() {
                $scope.isProcessing = false;
            });

            this.refreshCategoryCombo();
        };
        $scope.categoriesEmpty = function(){
            return Object.keys($scope.categories.data).length === 0;
        };
        $scope.validarComboCategorias = function(){
            if($scope.categoriesEmpty){
                alertService.error({message: $scope.msgErrorCategorias});
            }
        };

        $scope.refreshCategoryCombo = function() {
            superiorInstanceService.getCategories().then(function(response) {
                $scope.categories = {};
                $scope.forense = {};

                  _.forEach(response.data, function(value, key) {

                    if (key!== "AM" && key!== "AM_DVI" && key!== "PM" && key!== "PM_DVI" )
                    { var n = key;
                        $scope.forense[n] = value;}

                });
                $scope.categories.data = $scope.forense;

            }, function() {
                $scope.categories = {};
                $scope.categories.data = {};
            });
        };
        $scope.autoConfigurar = function() {
            //Search defaults
            if(!_.isUndefined($scope.categoriesMappings) && !_.isUndefined($scope.categories) && !_.isUndefined($scope.categories.data)){
                var categoriesSup =_.flatMap(_.toArray($scope.categories.data), function (element) {
                    return element.subcategories;
                });
                var categoriesSupObj = _.keyBy(categoriesSup, 'id');
                $scope.categoriesMappings.forEach(function(element){
                    if((_.isUndefined(element.idSuperior) || _.isEmpty(element.idSuperior))  && !_.isUndefined(categoriesSupObj[element.id])){
                        element.idSuperior = element.id;
                    }
                });
            }
        };
        $scope.saveConnections = function() {
            $scope.isProcessing = true;
            // if($scope.changed){
                $scope.connections.pki = $scope.connections.superiorInstance;
                superiorInstanceService.updateConnections($scope.connections).then(function() {
                    $scope.isProcessing = false;
                    alertService.success({message: 'Se ha guardado correctamente'});
                    // $route.reload();
                    $scope.refreshCategoryCombo();

                }, function(response) {
                    $scope.isProcessing = false;
                    alertService.error({message: 'Hubo un error: ' + response.data.message});
                });
            // }
        };
        $scope.saveMappings = function() {
            $scope.isProcessing = true;

            var saveMappingsList = [];
            var requestSaveMappings = {};

            for(var i = 0; i < $scope.categoriesMappings.length ; i++){
                var mapping = {};
                mapping.id = $scope.categoriesMappings[i].id;
                mapping.idSuperior = $scope.categoriesMappings[i].idSuperior;
                // if($scope.categoriesMappings[i].changed){
                    saveMappingsList.push(mapping);
                // }
            }
            requestSaveMappings.categoryMappingList = saveMappingsList;
            if(saveMappingsList.length > 0){
                superiorInstanceService.insertOrUpdateCategoriesMapping(requestSaveMappings).then(function() {
                    $scope.isProcessing = false;
                    alertService.success({message: 'Se ha guardado correctamente'});
                    for(var i = 0; i < $scope.categoriesMappings.length ; i++){
                        $scope.categoriesMappings[i].changed = false;
                    }
                }, function(response) {
                    $scope.isProcessing = false;
                    alertService.error({message: 'Hubo un error: ' + response.data.message});
                });
            }

        };
        $scope.checkConnection = function(url) {
            $scope.isProcessing = true;
            return superiorInstanceService.getConnectionStatus(url).then(function() {
                $scope.isProcessing = false;
                alertService.success({message: 'Se conecta correctamente'});
            }, function(response) {
                $scope.isProcessing = false;
                alertService.error({message: response.data.message});
            });
        };
        $scope.checkConnectionSupIns = function() {
            $scope.checkConnection($scope.connections.superiorInstance);
        };
        $scope.checkConnectionPki = function() {
            $scope.checkConnection($scope.connections.pki);
        };

        $scope.connect = function() {
            $scope.connections.pki = $scope.connections.superiorInstance;
            superiorInstanceService.updateConnections($scope.connections).then(function() {
                $scope.isProcessing = false;

                $scope.refreshCategoryCombo();

                superiorInstanceService.connect().then(function() {
                    $scope.isProcessing = false;
                    alertService.success({message: 'Se conectó correctamente'});
                }, function(response) {
                    $scope.isProcessing = false;
                    alertService.error({message: response.data.message });
                });

            }, function(response) {
                $scope.isProcessing = false;
                alertService.error({message: 'Hubo un error: ' + response.data.message});
            });

        };

        $scope.init();

    }

    return SuperiorInstanceController;

});
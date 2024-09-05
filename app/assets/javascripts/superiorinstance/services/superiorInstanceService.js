define([],function() {
    'use strict';

    function SuperiorInstanceService(playRoutes, $http) {

        this.getConnections = function () {
            console.log('GET CONNECTIONS');
            return $http.get('/connections');
            //return playRoutes.controllers.Interconnections.getConnections().get();
        };

        this.getConnectionStatus = function (url) {
            console.log('GET CONNECTION STATUS');
            return $http.get('/connections/status', { params: { url: url } });
            //return playRoutes.controllers.Interconnections.getConnectionStatus(url).get();
        };

        this.updateConnections = function (connections) {
            console.log('UPDATE CONNECTIONS');
            return $http.put('/connections', connections);
            //return playRoutes.controllers.Interconnections.updateConnections().put(connections);
        };

        this.listCategoriesMapping = function () {
            console.log('LIST CATEGORIES MAPPING');
            return $http.get('/categories-mapping');
            //return playRoutes.controllers.Categories.listCategoriesMapping().get();
        };

        this.insertOrUpdateCategoriesMapping = function (categoriesMappings) {
            console.log('INSERT OR UPDATE CATEGORIES MAPPING');
            return $http.put('/categories-mapping', categoriesMappings);
            //return playRoutes.controllers.Categories.insertOrUpdateCategoriesMapping().put(categoriesMappings);
        };

        this.getCategories = function() {
            console.log('GET CATEGORIES');
            return $http.get('/superior/category-tree-consumer');
            //return playRoutes.controllers.Interconnections.getCategoryTreeComboConsumer().get();
        };

        this.connect = function() {
            console.log('CONNECT');
            return $http.post('/connection');
            //return playRoutes.controllers.Interconnections.insertConnection().post();
        };
    }

    return SuperiorInstanceService;

});
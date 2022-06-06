define([],function() {
    'use strict';

    function SuperiorInstanceService(playRoutes) {

        this.getConnections = function () {
            return playRoutes.controllers.Interconnections.getConnections().get();
        };
        this.getConnectionStatus = function (url) {
            return playRoutes.controllers.Interconnections.getConnectionStatus(url).get();
        };
        this.updateConnections = function (connections) {
            return playRoutes.controllers.Interconnections.updateConnections().put(connections);
        };
        this.listCategoriesMapping = function () {
            return playRoutes.controllers.Categories.listCategoriesMapping().get();
        };
        this.insertOrUpdateCategoriesMapping = function (categoriesMappings) {
            return playRoutes.controllers.Categories.insertOrUpdateCategoriesMapping().put(categoriesMappings);
        };
        this.getCategories = function() {
            return playRoutes.controllers.Interconnections.getCategoryTreeComboConsumer().get();
        };
        this.connect = function() {
            return playRoutes.controllers.Interconnections.insertConnection().post();
        };
    }

    return SuperiorInstanceService;

});
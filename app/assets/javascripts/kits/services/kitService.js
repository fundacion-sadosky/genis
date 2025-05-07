define([],function() {
    'use strict';

    function KitService(playRoutes, $http) {

        this.get = function(id){
            return playRoutes.controllers.StrKits.get(id).get();
        };

        this.getFull = function(id){
            return playRoutes.controllers.StrKits.getFull(id).get();
        };

        this.listFull = function () {
            return playRoutes.controllers.StrKits.listFull().get();
        };

        this.list = function () {
            return playRoutes.controllers.StrKits.list().get();
        };

        this.add = function (kit) {
            return playRoutes.controllers.StrKits.add().post(kit);
        };

        this.updateKit = function(kit){
            return playRoutes.controllers.StrKits.update().put(kit);
        };

        this.exportKits = function () {
            return playRoutes.controllers.StrKits.exportKits().get();
        };

        this.importKits = function(formData) {
            // Extract the URL from the Play routes object.
            var url = playRoutes.controllers.StrKits.importKits().url;

            // Use $http directly to ensure proper FormData handling.
            return $http.post(url, formData, {
                transformRequest: angular.identity,
                headers: { 'Content-Type': undefined }  // Let the browser set multipart/form-data with boundary.
            });
        };


        this.delete = function (id) {
            return playRoutes.controllers.StrKits.delete(id).delete();
        };

    }

    return KitService;

});
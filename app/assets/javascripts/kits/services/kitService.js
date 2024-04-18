define([],function() {
    'use strict';

    function KitService(playRoutes) {

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

        this.delete = function (id) {
            return playRoutes.controllers.StrKits.delete(id).delete();
        };

    }

    return KitService;

});
define([],function() {
    'use strict';

    function KitService(playRoutes, $http) {

        this.get = function(id){
            console.log('GET KIT');
            return $http.get('/strkits/' + id);
            //return playRoutes.controllers.StrKits.get(id).get();
        };

        this.getFull = function(id){
            console.log('GET KIT FULL');
            return $http.get('/strkits-full/' + id);
            //return playRoutes.controllers.StrKits.getFull(id).get();
        };

        this.listFull = function () {
            console.log('GET LIST FULL');
            return $http.get('/strkits-full');
            //return playRoutes.controllers.StrKits.listFull().get();
        };

        this.list = function () {
            console.log('LIST KITS');
            return $http.get('/strkits');
            //return playRoutes.controllers.StrKits.list().get();
        };

        this.add = function (kit) {
            console.log('CREATE STRKIT');
            return $http.post('/strkit/create', kit);
            //return playRoutes.controllers.StrKits.add().post(kit);
        };

        this.updateKit = function(kit){
            console.log('UPDATE KIT');
            return $http.put('/strkit/update', kit);
            //return playRoutes.controllers.StrKits.update().put(kit);
        };

        this.delete = function (id) {
            console.log('DELETE KIT');
            return $http.delete('/strkit/delete/' + id);
            //return playRoutes.controllers.StrKits.delete(id).delete();
        };

    }

    return KitService;

});
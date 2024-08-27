define([],function() {
    'use strict';

    function MotiveService(playRoutes) {

        this.getMotivesTypes = function () {
            return playRoutes.controllers.MotiveController.getMotivesTypes().get();
        };
        this.getMotives = function(id)  {
            return playRoutes.controllers.MotiveController.getMotives(id,true).get();
        };
        this.updateMotive = function(motive)  {
            return playRoutes.controllers.MotiveController.update().put(motive);
        };
        this.insertMotive = function(motive)  {
            console.log('INSERT MOTIVE');
            return $http.post('/motive', motive);
            //return playRoutes.controllers.MotiveController.insert().post(motive);
        };
        this.deleteMotiveById = function(id)  {
            return playRoutes.controllers.MotiveController.deleteMotiveById(id).delete();
        };
    }

    return MotiveService;

});
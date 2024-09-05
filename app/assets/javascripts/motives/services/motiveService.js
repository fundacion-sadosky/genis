define([],function() {
    'use strict';

    function MotiveService(playRoutes) {

        this.getMotivesTypes = function () {
            console.log('GET  MOTIVES TYPES');
            return $http.get('/motive-types');
            //return playRoutes.controllers.MotiveController.getMotivesTypes().get();
        };
        this.getMotives = function(id)  {
            console.log('GET MOTIVES');
            return $http.get('/motive', { params: { id: id, abm: true } });
            //eturn playRoutes.controllers.MotiveController.getMotives(id,true).get();
        };
        this.updateMotive = function(motive)  {
            console.log('UPDATE MOTIVE');
            return $http.put('/motive', motive);
            //return playRoutes.controllers.MotiveController.update().put(motive);
        };
        this.insertMotive = function(motive)  {
            console.log('INSERT MOTIVE');
            return $http.post('/motive', motive);
            //return playRoutes.controllers.MotiveController.insert().post(motive);
        };
        this.deleteMotiveById = function(id)  {
            console.log('DELETE MOTIVE BY ID');
            return $http.delete('/motive/' + id);
            //return playRoutes.controllers.MotiveController.deleteMotiveById(id).delete();
        };
    }

    return MotiveService;

});
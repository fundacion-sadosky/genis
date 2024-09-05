define([],function() {
    'use strict';

    function ProfileApprovalService(playRoutes, $http) {

        this.getPendingProfiles = function (page,pageSize) {
            return $http.get('/superior/profile/approval', {
                params: {
                    page: page,
                    pageSize: pageSize
                }
            });
            //return playRoutes.controllers.Interconnections.getPendingProfiles(page,pageSize).get();
        };

        this.getTotalPendingProfiles = function () {
            return $http.get('/superior/profile/approval-total');
            //return playRoutes.controllers.Interconnections.getTotalPendingProfiles().get();
        };

        this.approveProfiles = function (data) {
            console.log('APPROVE PROFILES');
            return $http.post('/superior/profile/approval', data);
            //return playRoutes.controllers.Interconnections.approveProfiles().post(data);
        };

        this.rejectPendingProfile = function (id,res) {
            return $http.delete('/superior/profile/approval', {
                params: {
                    id: id,
                    motive: res.motive,
                    idMotive: parseInt(res.idMotive)
                }
            });
            //return playRoutes.controllers.Interconnections.rejectPendingProfile(id,res.motive,parseInt(res.idMotive)).delete();
        };

        this.getMotives = function()  {
            var motiveTypeReject =  1;
            return $http.get('/motive', { params: { id: 1, abm: false } });
            //return playRoutes.controllers.MotiveController.getMotives(motiveTypeReject,false).get();
        };
    }

    return ProfileApprovalService;

});
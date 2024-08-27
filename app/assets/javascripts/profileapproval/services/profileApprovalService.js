define([],function() {
    'use strict';

    function ProfileApprovalService(playRoutes, $http) {

        this.getPendingProfiles = function (page,pageSize) {
            return playRoutes.controllers.Interconnections.getPendingProfiles(page,pageSize).get();
        };
        this.getTotalPendingProfiles = function () {
            return playRoutes.controllers.Interconnections.getTotalPendingProfiles().get();
        };
        this.approveProfiles = function (data) {
            console.log('APPROVE PROFILES');
            return $http.post('/superior/profile/approval', data);
            //return playRoutes.controllers.Interconnections.approveProfiles().post(data);
        };
        this.rejectPendingProfile = function (id,res) {
            return playRoutes.controllers.Interconnections.rejectPendingProfile(id,res.motive,parseInt(res.idMotive)).delete();
        };
        this.getMotives = function()  {
            var motiveTypeReject =  1;
            return playRoutes.controllers.MotiveController.getMotives(motiveTypeReject,false).get();
        };
    }

    return ProfileApprovalService;

});
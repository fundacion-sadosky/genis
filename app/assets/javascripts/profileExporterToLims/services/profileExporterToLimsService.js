define([],function() {
    'use strict';

    function ProfileExporterToLimsService(playRoutes,userService, $http) {

        this.exporterArchivesLims = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            console.log('EXPORTER ARCHIVES LIMS');
            return $http.post('/profile-exportToLims', search);
            //return playRoutes.controllers.Profiles.exporterLimsFiles().post(search);
        };

    }

    return ProfileExporterToLimsService;

});
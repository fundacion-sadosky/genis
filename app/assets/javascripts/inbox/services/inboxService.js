define([], function() {
    'use strict';

    function InboxService(playRoutes, $http) {

        this.count = function(searchObject){
            console.log('COUNT NOTIFICATIONS');
            return $http.post('/notifications/total', searchObject);
            //return playRoutes.controllers.Notifications.count().post(searchObject);
        };
        
        this.search = function(searchObject){
            console.log('SEARCH NOTIFICATIONS');
            return $http.post('/notifications/search', searchObject);
            //return playRoutes.controllers.Notifications.search().post(searchObject);
        };

        this.changeFlag = function(id, flagged){
            console.log('CHANGE FLAG');
            return $http.post('/notifications/flag/' + id, {flag: flagged});
            //return playRoutes.controllers.Notifications.changeFlag(id, flagged).post();
        };

        this.delete = function(id){
            console.log('DELETE FLAG');
            return $http.delete('/notifications/' + id);
            //return playRoutes.controllers.Notifications.delete(id).delete();
        };

    }

    return InboxService;
});
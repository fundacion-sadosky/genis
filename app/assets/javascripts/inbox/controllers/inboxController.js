define([], function() {
    'use strict';

    function InboxController($scope, $location, inboxService, alertService, $q, userService) {
        $scope.showOptions = [{label: 'Todo', filters: {}, helptip: 'inbox.show.all', placement: 'top'},
            {label: 'Pendiente', filters: {pending: true}, helptip: 'inbox.show.pending', placement: 'bottom'},
            {label: 'Marcado', filters: {flagged: true}, helptip: 'inbox.show.flagged', placement: 'right'}];
        $scope.activeOption = 0;
        $scope.status = {};
        $scope.status.open =true;
        $scope.search = {};

        $scope.pageSize = 30;
        $scope.currentPage = 1;

        $scope.sortField = 'date';
        $scope.ascending = false;

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        var createSearchObject = function(filters) {
            var searchObject = {};
            searchObject.user = userService.getUser().name;
            if (filters.kind) {
                searchObject.kind = filters.kind;
            }
            if (filters.flagged === true) {
                searchObject.flagged = true;
            }
            if (filters.pending === true) {
                searchObject.pending = true;
            }

                if(filters.hourFrom !== undefined){
                    searchObject.hourFrom = filters.hourFrom;
                searchObject.hourFrom.setHours(0);
                searchObject.hourFrom.setMinutes(0);
                searchObject.hourFrom.setSeconds(0);
            }

            if (filters.hourUntil !== undefined) {
                searchObject.hourUntil = filters.hourUntil;
                searchObject.hourUntil.setHours(23);
                searchObject.hourUntil.setMinutes(59);
                searchObject.hourUntil.setSeconds(59);
            }
            searchObject.page = $scope.currentPage - 1;
            searchObject.pageSize = $scope.pageSize;
            searchObject.sortField = $scope.sortField;
            searchObject.ascending = $scope.ascending;
            return searchObject;
        };

        $scope.searchNotifications = function(filters) {
            $scope.search = filters;
            var searchObject = createSearchObject(filters);
            inboxService.count(searchObject).then(function(response){
                $scope.totalItems = response.headers('X-NOTIF-LENGTH');
                $scope.isProcessing = true;
                inboxService.search(searchObject).then(function(response) {
                    $scope.notifications = response.data;
                    $scope.isProcessing = false;
                    $scope.noResult = $scope.notifications.length === 0;
                }, function() {
                    $scope.isProcessing = false;
                });
            });
            
        };

        $scope.resetSearch = function(index) {
            $scope.activeOption = index;
            $scope.currentPage = 1;
            $scope.sortField = 'date';
            $scope.ascending = false;
        };

        $scope.show = function(index) {
            $scope.resetSearch(index);
            $scope.searchNotifications($scope.showOptions[index].filters);
        };

        // by default show everything
        $scope.show(0);

        $scope.changePage = function() {
            $scope.searchNotifications($scope.search);
        };

        $scope.sortBy = function(field) {
            $scope.currentPage = 1;
            $scope.sortField = field;
            $scope.ascending = !$scope.ascending;
            $scope.searchNotifications($scope.search);
        };

        $scope.goTo = function(url) {
            $location.url(url);
        };
        
        $scope.changeFlag = function(notification) {
            notification.flagged = !notification.flagged;
            inboxService.changeFlag(notification.id, notification.flagged).then(
                function() {
                    alertService.success({message: 'La notificación fue modificada exitosamente.'});
                },
                function(response) {
                    alertService.error({message: response.data});
                    notification.flagged = !notification.flagged;
                }
            );
        };
        
        var doOnSelected = function(f, success, error) {
            var promises = [];
            $scope.notifications.forEach(function (n) {
                if (n.selected) {
                    // if (n.selected && !n.pending) {
                    promises.push(f(n.id));
                }
            });
            $q.all(promises).then(
                function () {
                    alertService.success({message: success});
                    $scope.searchNotifications($scope.search);
                },
                function () {
                    alertService.error({message: error});
                }
            );
        };

        $scope.deleteSelected = function() {
            doOnSelected(inboxService.delete, 
                'Las notificaciones fueron borradas exitosamente',
                'Hubo un error al eliminar las notificaciones');
        };

        $scope.changeSelection = function() {
            $scope.notifications.forEach(function(n) {
                // if(!n.pending) {
                    n.selected = $scope.selectAll;
                // }
            });
        };
    }
    
    return InboxController;

});

define(['angular', 'lodash', 'jquery'], function (angular, _, $) {
  'use strict';

  function PedigreeMatchesCourtCaseController(
    $scope,
    pedigreeMatchesService,
    matchesService,
    alertService,
    profiledataService,
    $route,
    pedigreeService,
    pedigreeMatchesGroupsService,
    userService,
    profileService
  ) {
    $scope.isProcessing = false;
    $scope.noResult = true;
    $scope.search = {};
    $scope.previousSearch = {};
    $scope.idCourtCase = parseInt($route.id);

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

    $scope.status = [
      {label: "todos", value: "", index: 0},
      {label: $.i18n.t('generics.pendingPlural'), value: "pending", index: 1},
      {label: $.i18n.t('generics.confirmedPlural'), value: "hit", index: 2},
      {label: $.i18n.t('generics.discardedPlural'), value: "discarded", index: 3}
    ];
    $scope.tabFiltro = $scope.status[1].value;
    $scope.indexFiltro = $scope.status[1].index;
    $scope.clase = "tab-estados-tab__pendientes";

    $scope.matchPStatus = matchesService.getMatchStatusEnum();

    $scope.datepickers = {
      hourFrom: false,
      hourUntil: false
    };

    $scope.dateOptions = {
      initDate: new Date()
    };

    $scope.currentPage = 1;
    $scope.pageSize = 30;

    $scope.grupoId = 'pedigree';

    $scope.currentPages = 1;
    $scope.tamaPagina = 30;

    $scope.sortFields = 'date';
    $scope.ascendings = false;

    $scope.itemsTotales = '0';

    var user = userService.getUser();
    var userName = user.name;

    $scope.printSummaryReport = function () {
      var head = '<head><title>Resumen caso ' + $scope.idCourtCase + '</title>';
      $("link").each(function () {
        head += '<link rel="stylesheet" href="' + $(this)[0].href + '" />';
      });
      head += "</head>";
      $scope.$apply();
      var report = window.open('', '_blank');
      report.document.write(
        '<html>' + head +
        '<body>' +
        $('#report_' + $scope.idCourtCase).html() +
        '</body></html>'
      );
      report.document.close();
      $(report).on('load', function () {
        report.print();
        report.close();
      });
    };

    $scope.toggleDatePicker = function ($event, witch) {
      $event.preventDefault();
      $event.stopPropagation();

      $scope.datepickers[witch] = !$scope.datepickers[witch];
    };


    var createSearchObjectTarjeta = function (filters) {
      var searchObject = {};

      if (filters.profile && filters.profile.length > 0) {
        searchObject.profile = filters.profile;
      }

      if (filters.category && filters.category.length > 0) {
        searchObject.category = filters.category;
      }

      searchObject.caseType = $scope.caseType;

      searchObject.idCourtCase = $scope.idCourtCase;

      searchObject.hourFrom = filters.hourFrom;
      if (searchObject.hourFrom) {
        searchObject.hourFrom.setHours(0);
        searchObject.hourFrom.setMinutes(0);
        searchObject.hourFrom.setSeconds(0);
      }

      searchObject.hourUntil = filters.hourUntil;
      if (searchObject.hourUntil) {
        searchObject.hourUntil.setHours(23);
        searchObject.hourUntil.setMinutes(59);
        searchObject.hourUntil.setSeconds(59);
      }

      if ($scope.tabFiltro && $scope.tabFiltro.length > 0) {
        searchObject.status = $scope.tabFiltro;
      }
      searchObject.page = $scope.currentPage - 1;
      searchObject.pageSize = $scope.pageSize;
      searchObject.group = $scope.grupoId;
      searchObject.pageSizeMatch = $scope.tamaPagina;
      searchObject.pageMatch = $scope.currentPages - 1;

      searchObject.sortField = $scope.sortFields;
      searchObject.ascending = $scope.ascendings;

      return searchObject;
    };

    $scope.findMatches = function (filters) {
      $scope.isProcessing = true;
      $scope.noResult = false;
      var searchObject = createSearchObjectTarjeta(filters);

      pedigreeMatchesService.countMatches(searchObject).then(function (response) {
        $scope.totalItems = response.headers('X-MATCHES-LENGTH');

        if ($scope.totalItems !== '0') {
          pedigreeMatchesService.findMatchesPedigree(searchObject).then(function (response) {
            $scope.matches = response.data;
            $scope.isProcessing = false;
          }, function () {
            $scope.isProcessing = false;
          });
        } else {
          $scope.noResult = true;
          $scope.isProcessing = false;
        }

      }, function () {
        $scope.isProcessing = false;
      });
    };

    $scope.clearSearch = function () {
      $scope.search = {};
      $scope.previousSearch = {};
      $scope.findMatches($scope.previousSearch);
    };

    $scope.searchMatches = function () {
      $scope.previousSearch = angular.copy($scope.search);
      $scope.findMatches($scope.previousSearch);
    };

    pedigreeService.getCourtCaseFull($scope.idCourtCase).then(function (response) {
      $scope.caseType = response.data.caseType;

      if ($scope.caseType === "MPI") {
        $scope.isMpi = true;

      } else {
        $scope.isMpi = false;
      }
      $scope.findMatches({});
    });


    $scope.searchPrevious = function (grupo) {
      $scope.grupoId = grupo;
      $scope.findMatches($scope.previousSearch);
    };

    $scope.maxDate = $scope.maxDate ? null : new Date();
    $scope.minDateCoin = null;

    $scope.checkMaxMin = function (fieldName, fechaMin) {
      var aux = $scope.search[fieldName];
      var min = $scope.search[fechaMin];
      var max = new Date();

      if (min === undefined || min === null) {
        min = $scope.search.hourFrom;
      }
      if (max - aux < 0) {
        alertService.info({message: $.i18n.t('alerts.date.afterNow')});
        $scope.search[fieldName] = undefined;
      } else {
        if (min - aux > 0) {
          alertService.info({message: $.i18n.t('alerts.date.after')});
          $scope.search[fieldName] = undefined;
        }
      }

    };

    $scope.checkMax = function (fieldName) {
      var aux = $scope.search[fieldName];
      var today = new Date();

      if (today - aux < 0) {
        alertService.info({message: $.i18n.t('alerts.date.before')});
        $scope.search[fieldName] = undefined;
        $scope.minDateCoin = null;
      } else {
        if (fieldName === 'hourFrom') {
          if (aux !== undefined || aux !== null) {
            $scope.minDateCoin = aux;
          } else {
            $scope.minDateCoin = null;
          }
        }
      }

    };

    $scope.clearMatches = function () {
      $scope.matches = [];
      $scope.noResult = false;
    };

    $scope.filtros = function (filtro) {
      var clase = "tab-estados-tab__" + filtro.label;
      if (clase !== $scope.clase) {
        $("div." + $scope.clase).removeClass($scope.clase + "__selected");
        $scope.clase = clase;
      }
      $scope.tabFiltro = filtro.value;
      $scope.indexFiltro = filtro.index;

      $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
      $scope.searchMatches();
    };

    profiledataService.getCategories().then(function (response) {
      $scope.cate = response.data;
      $scope.categorias = {};
      $scope.grupo = {};
      _.forEach($scope.cate, function (value, key) {
        if ($scope.caseType === "MPI") {
          if (key === "AM" || key === "PM") {
            var n = key;
            $scope.categorias[n] = value;
          }
        } else {
          if (key === "AM_DVI" || key === "PM_DVI") {
            var m = key;
            $scope.categorias[m] = value;
          }
        }
      });
    });


    $scope.sortBy = function (group, sortField) {
      $scope.currentPage = 1;
      if (sortField !== $scope.sortField) {
        $scope.ascending = true;
      } else {
        $scope.ascending = !$scope.ascending;
      }
      $scope.sortField = sortField;

      $scope.searchMatches(group);
    };

    $scope.discard = function (confirm, jsonMatch) {
      if (!confirm) {
        return;
      }

      var match = $scope.matches.filter(function (m) {
        return m.matchCardPed.filter(function (card) {
          return card.id.$oid === JSON.parse(jsonMatch).id.$oid;
        })[0];
      })[0];

      pedigreeMatchesGroupsService.discard(match.matchCardPed[0].id.$oid).then(function () {
        if (userName === match.matchCardPed[0].assignee) {
          match.matchCard.estado = 'discarded';
          match.matchCardPed[0].estado = 'discarded';
        }
        if (user.superuser) {
          match.matchCard.estado = 'discarded';
          match.matchCardPed[0].estado = 'discarded';
        }
        alertService.success({message: $.i18n.t('alerts.match.discardSuccess')});
        $scope.searchMatches();
      }, function (response) {
        alertService.error(response.data);
      });
    };

    $scope.canDiscard = function (match) {
      return pedigreeMatchesGroupsService.canDiscardCourtCase(match, user);
    };

    $scope.isHit = function (match) {
      return pedigreeMatchesGroupsService.isHitCourtCase(match);
    };

    $scope.goToComparisonScreening = function (m, match) {
      if (m.matchingId === "") {
        return;
      }
      if ($scope.grupoId === "pedigree") {
        return '/#/comparison/' + m.mtProfile + '/matchedProfileId/' + m.internalCode + '/matchingId/' + m.matchingId + '?isScreening=true';
      } else {
        return '/#/comparison/' + m.mtProfile + '/matchedProfileId/' + match.internalCode + '/matchingId/' + m.matchingId + '?isScreening=true';

      }

    };
    $scope.goToScenario = function (m, matchCard) {
      if ($scope.grupoId === "pedigree") {
        return '/#/pedigree/' + $scope.idCourtCase + '/' + matchCard.id + '?u=' + m.unknown + '&p=' + m.internalCode + '&m=' + m.id.$oid;
      } else {
        return '/#/pedigree/' + $scope.idCourtCase + '/' + m.idPedigree + '?u=' + m.unknown + '&p=' + matchCard.id + '&m=' + m.id.$oid;
      }

    };
  }

  return PedigreeMatchesCourtCaseController;
});


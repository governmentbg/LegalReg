package com.ib.urireg.beans;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.search.JobHistorySearch;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.quartz.*;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Clob;
import java.util.*;

/**
 * СИСТЕМНИ ПРОЦЕСИ
 *
 * @author silvia
 */

@Named
@ViewScoped
public class QuartzInfoBean extends IndexUIbean {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzInfoBean.class);

    private static final Set<String> ALLOWED_GROUPS = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("SystemTriggersIB")));
    private static final String QUARTZ_KEY = QuartzInitializerListener.QUARTZ_FACTORY_KEY;


    private static final Map<Integer, String> IMAGE_SOURCES = new HashMap<>();
    private static final Map<Integer, String> IMAGE_ALTS = new HashMap<>();

    static {
        IMAGE_SOURCES.put(1, "ready.png");
        IMAGE_SOURCES.put(2, "warning.png");
        IMAGE_SOURCES.put(3, "error.png");
        IMAGE_SOURCES.put(4, "readyEmpty.png");

        IMAGE_ALTS.put(1, "Изпълнена");
        IMAGE_ALTS.put(2, "Внимание");
        IMAGE_ALTS.put(3, "Грешка");
        IMAGE_ALTS.put(4, "Без информация");
    }

    private Scheduler scheduler;

    private List<IBTriggers> allTrigger = new ArrayList<>();
    private List<String[]> testList = new ArrayList<>(); // TODO това какво е?
    private IBTriggers selectedTrigger;
    private transient Object[] selectedHistory;

    /**
     * ИСТОРИЯ НА JOB-ОВЕТЕ */
    private Integer period;
    private Date dateOt;
    private Date dateDo;

    private transient JobHistorySearch jobSearch;
    private LazyDataModelSQL2Array jobHistory;



    @PostConstruct
    public void init() {

        LOGGER.info("SYSTEM QUARTZ INIT!!!!");
        try {

            loadTriggersAndJobs();

        } catch (Exception e) {
            LOGGER.error("Грешка при инициализиране на системни процеси", e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при инициализиране на системни процеси!");
        }
    }


    /**
     * ЗАРЕЖДА СПИСЪК С ВСИЧКИ ТРИГОВЕ И ДЖОБОВЕ
     */
    private void loadTriggersAndJobs() {
        try {

            FacesContext facesContext = FacesContext.getCurrentInstance();
            StdSchedulerFactory factory = (StdSchedulerFactory) facesContext
                    .getExternalContext()
                    .getApplicationMap()
                    .get(QUARTZ_KEY);

            Scheduler scheduler = factory.getScheduler();
            setScheduler(scheduler);

            getTestList().add(new String[]{"13", "14", "15"});

            allTrigger.clear();
            for (String groupName : scheduler.getJobGroupNames()) {
                LOGGER.debug("Group: {}", groupName);

                if (!ALLOWED_GROUPS.contains(groupName)) {
                    continue;
                }

                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);

                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    if (triggers.isEmpty()) {
                        continue;
                    }

                    Trigger trigger = triggers.get(0);
                    TriggerKey triggerKey = trigger.getKey();

                    allTrigger.add(new IBTriggers(
                            triggerKey.getName(),
                            groupName,
                            triggerKey,
                            scheduler.getTriggerState(triggerKey),
                            trigger.getPreviousFireTime(),
                            trigger.getNextFireTime(),
                            jobKey.getName(),
                            jobKey.getGroup(),
                            jobKey,
                            jobDetail != null ? jobDetail.getDescription() : null
                    ));
                }
            }

        } catch (Exception e) {
            LOGGER.error("Грешка при зареждане на тригери и джобове: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Проблем с инициализацията на Quartz Scheduler");
        }
    }


    /**
     * В зависимост от състоянието на тригера - Pause/Resume ако е в стейт != от NORMAL/PAUSE - не прави нищо
     */
    public void actionPauseResumeTrg(String trgName, String trgGroup) {
        LOGGER.debug("actionPauseResumeTrg: trgName={}, trgGroup={}", trgName, trgGroup);

        TriggerKey trgKey = new TriggerKey(trgName, trgGroup);
        try {

            Trigger.TriggerState state = scheduler.getTriggerState(trgKey);

            if (state == Trigger.TriggerState.NORMAL) {
                scheduler.pauseTrigger(trgKey);
            } else if (state == Trigger.TriggerState.PAUSED) {
                scheduler.resumeTrigger(trgKey);
            }

            loadTriggersAndJobs();

        } catch (SchedulerException e) {
            LOGGER.error("Грешка при промяна на състоянието на тригера: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при промяна на състоянието на тригера: " + e.getMessage());
        }
    }

    public void actionFireTrigger(String jobName, String jobGroup) {
        LOGGER.debug("actionFireTrigger:jobName:{},jobGroup {}", jobName, jobGroup);

        try {
            JobKey jobKey = new JobKey(jobName, jobGroup);
            scheduler.triggerJob(jobKey);

            loadTriggersAndJobs();

        } catch (SchedulerException e) {
            LOGGER.error("Грешка при промяна на състиянието на тригера {}", e.getMessage());
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при промяна на състиянието на тригера" + e.getMessage());
        }

    }

    public void actionStartStopScheduler() {
        try {

            if (scheduler.getMetaData().isStarted()) {
                scheduler.shutdown();
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Scheduler was stopped");
            } else {
                scheduler.start();
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Scheduler was started");
            }
        } catch (SchedulerException e) {
            LOGGER.error("Грешка при стартиране/спиране на процеси " + e.getMessage());
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void actionStandbyOnOff() {
        try {

            if (!scheduler.getMetaData().isInStandbyMode()) {
                scheduler.standby();
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Scheduler was put in standby");
            } else {
                scheduler.start();
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Scheduler was started");
            }
        } catch (SchedulerException e) {
            LOGGER.error(e.getMessage());
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }


    /**
     * Дава историята от IBG_JOBS за конкретен job trgName
     * -то ни трябва единствено за да маркираме дали job-a минал регулярно или е стартиран извънредно
     * (ако IBG_JOB.TRG_NAME != trgName => e пуснат ръчно. По някаква причина при извънредно пускане TRIGER_NAME е някакво странно
     */
    public void actionGetJobHistory(Date startTime, Date endTime, String jobName, String trgName) {
        LOGGER.info("actionGetJobHistory: jobName={}, trgName={}", jobName, trgName);

        if (isNullOrEmpty(jobName) || isNullOrEmpty(trgName)) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "actionGetJobHistory: Empty parameter");
            return;
        }

        try {

            this.jobSearch.setJobKeyName(jobName);
            this.jobSearch.setTrigKeyName(trgName);

            jobSearch.buildQuery();
            jobHistory = new LazyDataModelSQL2Array(jobSearch, "START_TIME desc");

        } catch (Exception e) {
            LOGGER.error("Грешка при търсене на история: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при търсене на история: " + e.getMessage());
        }
    }

    /**
     * Когато се зарежда информация за Job-a полето exception може да бъде CLOB..
     * при зареждане на екрана правя проверка дали е string или clob и го връщаме в четим вид
     */
    public String convertRowIfClob(Object txt) {

        if (txt == null) {
            return null;
        }

        try {

            if (txt instanceof Clob) {

                Clob clob = (Clob) txt;
                return SearchUtils.clobToString(clob);
            }

        } catch (Exception e) {
            LOGGER.error("Грешка при конвертиране на Clob: {}", e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
                    "Грешка при конвертиране на Clob: " + e.getMessage());
        }

        return String.valueOf(txt);
    }


    private boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }


    public void actionSearchTrigger() {
        LOGGER.info(this.selectedTrigger.getJobName());
        actionGetJobHistory(this.jobSearch.getStartTime(), this.jobSearch.getEndTime(), this.selectedTrigger.getJobName(), this.selectedTrigger.getTriggerName());
    }

    public String getImageSource(Integer historyStatus) {
        return getImageData(historyStatus, IMAGE_SOURCES, "readyEmpty.png");
    }

    public String getImageSourceAlt(Integer historyStatus) {
        return getImageData(historyStatus, IMAGE_ALTS, "Без информация");
    }

    private String getImageData(Integer historyStatus, Map<Integer, String> dataMap, String defaultValue) {
        if (historyStatus == null) {
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Невалиден параметър");
            return null;
        }
        return dataMap.getOrDefault(historyStatus, defaultValue);
    }

    public void actionSelectTrigger() {

        if (this.jobSearch == null) {
            this.jobSearch = new JobHistorySearch();
        }

        setPeriod(13);
        actionPeriod();
        actionSearchTrigger();
    }


    public String actionPeriod() {

        if (this.period != null) {
            Date[] di;
            di = DateUtils.calculatePeriod(this.period);
            this.jobSearch.setStartTime(di[0]);
            this.jobSearch.setEndTime(di[1]);

        } else {
            this.jobSearch.setStartTime(null);
            this.jobSearch.setEndTime(null);
        }
        return null;
    }

    public void changeDate() {
        this.setPeriod(null);
    }




    /********************************************** GETTERS/SETTERS *****************************************************************/
    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public List<IBTriggers> getAllTrigger() {
        LOGGER.debug("------------- get all triggers");
        return allTrigger;
    }

    public void setAllTrigger(List<IBTriggers> allTrigger) {
        this.allTrigger = allTrigger;
    }

    public List<String[]> getTestList() {
        return testList;
    }

    public void setTestList(List<String[]> testList) {
        this.testList = testList;
    }

    public IBTriggers getSelectedTrigger() {
        return selectedTrigger;
    }

    public void setSelectedTrigger(IBTriggers selectedTrigger) {
        this.selectedTrigger = selectedTrigger;
    }

    public Object[] getSelectedHistory() {
        return selectedHistory;
    }

    public void setSelectedHistory(Object[] selectedHistory) {
        this.selectedHistory = selectedHistory;
    }

    public Date getToday() {
        return new Date();
    }

    public Date getDateOt() {
        return dateOt;
    }

    public void setDateOt(Date dateOt) {
        this.dateOt = dateOt;
    }

    public Date getDateDo() {
        return dateDo;
    }

    public void setDateDo(Date dateDo) {
        this.dateDo = dateDo;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public JobHistorySearch getJobSearch() {
        return jobSearch;
    }

    public void setJobSearch(JobHistorySearch jobSearch) {
        this.jobSearch = jobSearch;
    }

    public LazyDataModelSQL2Array getJobHistory() {
        return jobHistory;
    }

    public void setJobHistory(LazyDataModelSQL2Array jobHistory) {
        this.jobHistory = jobHistory;
    }





    /********************************************** IBTriggers CLASS *****************************************************************/

    public class IBTriggers implements Serializable {

        private static final long serialVersionUID = -7312050128775328291L;

        private final String triggerName;
        private final String triggerGroup;
        private final TriggerKey triggerKey;
        private final Trigger.TriggerState triggerState;
        private final Date triggerPrevFireTime;
        private final Date triggerNextFireTime;
        private final String jobName;
        private final String jobGroup;
        private final JobKey jobKey;
        private final String jobDescription;

        public IBTriggers(String triggerName, String triggerGroup, TriggerKey triggerKey, Trigger.TriggerState triggerState, Date triggerPrevFireTime, Date triggerNextFireTime, String jobName, String jobGroup, JobKey jobKey, String jobDescription) {
            super();
            this.triggerName = triggerName;
            this.triggerGroup = triggerGroup;
            this.triggerKey = triggerKey;
            this.triggerState = triggerState;
            this.triggerPrevFireTime = triggerPrevFireTime;
            this.triggerNextFireTime = triggerNextFireTime;
            this.jobName = jobName;
            this.jobGroup = jobGroup;
            this.jobKey = jobKey;
            this.jobDescription = jobDescription;
        }

        public String getTriggerName() {
            return triggerName;
        }

        public String getTriggerGroup() {
            return triggerGroup;
        }

        public TriggerKey getTriggerKey() {
            return triggerKey;
        }

        public Trigger.TriggerState getTriggerState() {
            return triggerState;
        }

        public Date getTriggerPrevFireTime() {
            return triggerPrevFireTime;
        }

        public Date getTriggerNextFireTime() {
            return triggerNextFireTime;
        }

        public String getJobName() {
            return jobName;
        }

        public String getJobGroup() {
            return jobGroup;
        }

        public JobKey getJobKey() {
            return jobKey;
        }

        public String getJobDescription() {
            return jobDescription;
        }

    }
}

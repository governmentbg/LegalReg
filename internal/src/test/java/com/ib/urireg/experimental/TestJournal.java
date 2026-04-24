package com.ib.urireg.experimental;

public class TestJournal {
//	private static final Logger LOGGER = LoggerFactory.getLogger(TestJournal.class);
//
//	private static SystemData sd;
//
//	private static DocDAO		docDao;
//	private static DeloDAO		deloDao;
//	private static DeloDocDAO	deloDocDao;
//	private static DeloDeloDAO	deloDeloDao;
//	private static TaskDAO 		taskDao;
//	private Doc				doc;
//	@Test
//	public void alabala() {
//		LOGGER.info("This is INFO");
//		LOGGER.warn("This is WARN");
//		LOGGER.debug("This is DEBUG");
//		LOGGER.error("This is ERROR");
//	}
//
//	@BeforeClass
//	public static void setUp() {
//		docDao = new DocDAO(ActiveUser.DEFAULT);
//		deloDao = new DeloDAO(ActiveUser.DEFAULT);
//		deloDocDao = new DeloDocDAO(ActiveUser.DEFAULT);
//		deloDeloDao = new DeloDeloDAO(ActiveUser.DEFAULT);
//		taskDao = new TaskDAO(ActiveUser.DEFAULT);
//
//		sd = new SystemData();
//	}
//
//	@Test
//	public void testSaveNewDoc() throws Exception {
//		try {
//
//			  // 1. запис на документ this.doc = new Doc();
//
//			this.doc = new Doc();
//
//			this.doc.setRegistraturaId(1);
//			this.doc.setRegisterId(1);
//			this.doc.setOtnosno("alabala");
//
//			this.doc.setDocType(1);
//			this.doc.setDocVid(1);
//			this.doc.setDocDate(new Date());
//			this.doc.setFreeAccess(1);
//
//
//			  // запис на док със създаване на дело JPA.getUtil().runInTransaction(() ->
//			  JPA.getUtil().runInTransaction(() ->this.doc = docDao.save(this.doc, false, null, null, sd));
//			  LOGGER.info("--OK-- dao.save(this.doc, true, null, sd). ID="+this.doc.getId()+ " PNR="+this.doc.getPnr());
//			 assertNotNull(this.doc.getId());
//		} catch (BaseException e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
////		} catch (JAXBException e) {
////			LOGGER.error(e.getMessage(), e);
////			fail(e.getMessage());
////		} catch (IOException e) {
////			LOGGER.error(e.getMessage(), e);
////			fail(e.getMessage());
//		}
//
//	}
//	@Test
//	public void testModifyDoc() throws Exception {
//		try {
//
//		//====================
//			JPA.getUtil().runWithClose(() -> this.doc = docDao.findById(3067));
//			LOGGER.info("--OK-- dao.findById(this.doc.getId())ID="+this.doc.getId()+ " "+this.doc.getPnr());
//			this.doc.setOtnosno("baba meca 2");
//			JPA.getUtil().runInTransaction(() ->this.doc = docDao.save(this.doc, false, null, null, sd));
//			LOGGER.info("--OK2-- dao.findById(this.doc.getId())ID="+this.doc.getId()+ " PNR="+this.doc.getPnr());
////			String objectToXml = JAXBHelper.objectToXml(this.doc, true);
////			LOGGER.info(objectToXml);
//		} catch (BaseException e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
////		} catch (JAXBException e) {
////			LOGGER.error(e.getMessage(), e);
////			fail(e.getMessage());
////		} catch (IOException e) {
////			LOGGER.error(e.getMessage(), e);
////			fail(e.getMessage());
//		}
//	}
//
//	@Test
//	public void testDeleteDoc() throws Exception {
//		try {
//
//			//====================
//				JPA.getUtil().runWithClose(() -> this.doc = docDao.findById(3067));
//				LOGGER.info("--OK-- dao.findById(this.doc.getId())ID="+this.doc.getId()+ " "+this.doc.getPnr());
//
//				JPA.getUtil().runInTransaction(() -> docDao.deleteById(this.doc.getId()));
//				LOGGER.info("--OK2-- dao.findById(this.doc.getId())ID="+this.doc.getId()+ " PNR="+this.doc.getPnr());
////				String objectToXml = JAXBHelper.objectToXml(this.doc, true);
////				LOGGER.info(objectToXml);
//			} catch (BaseException e) {
//				LOGGER.error(e.getMessage(), e);
//				fail(e.getMessage());
////			} catch (JAXBException e) {
////				LOGGER.error(e.getMessage(), e);
////				fail(e.getMessage());
////			} catch (IOException e) {
////				LOGGER.error(e.getMessage(), e);
////				fail(e.getMessage());
//			}
//		}
//
//
//	@Test
//	public void testNewTask() throws Exception {
//		Task task=new Task();
//
//		task.setRegistraturaId(1);
//		task.setTaskType(1);
//		task.setStatus(1);
//		task.setStatusDate(new Date());
//		task.setStatusUserId(-1);
//		task.setAssignDate(new Date());
//		task.setSrokDate(new Date());
//		task.setTaskInfo("да се направи");
//
//		task.setDocRequired(1);
//		task.setCodeExecs(new ArrayList<>());
//
//		task.setCodeAssign(13);
//		task.setCodeControl(14);
//		task.getCodeExecs().add(9);
//
//		try {
//			JPA.getUtil().runInTransaction(() -> taskDao.save(task));
////			JPA.getUtil().begin();
////			task = taskDao.save(task, null, sd);
//			LOGGER.info("--OK-- dao.findById(taks.getId())ID="+task.getId()+ " "+task.getPnr());
////
////			JPA.getUtil().commit();
//		} catch (BaseException e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//	}
//
//
//	@Test
//	public void testKG() {
//		LOGGER.info("info");
//		LOGGER.debug("debug");
//				try {
//
//					  // 1. запис на документ this.doc = new Doc();
//
////					this.doc = new Doc();
////
////					this.doc.setRegistraturaId(1);
////					this.doc.setRegisterId(1);
////					this.doc.setOtnosno("alabala");
////
////					this.doc.setDocType(1);
////					this.doc.setDocVid(1);
////					this.doc.setDocDate(new Date());
////					this.doc.setFreeAccess(1);
////
////
////					  // запис на док със създаване на дело JPA.getUtil().runInTransaction(() ->
////					  JPA.getUtil().runInTransaction(() ->this.doc = dao.save(this.doc, false, null, null, sd));
////					  LOGGER.info("--OK-- dao.save(this.doc, true, null, sd). ID="+this.doc.getId()
////					  );
//
//					//====================
//					JPA.getUtil().runWithClose(() -> this.doc = docDao.findById(2901));
//					LOGGER.info("--OK-- dao.findById(this.doc.getId())ID="+this.doc.getId()+ " "+this.doc.getPnr());
//					this.doc.setOtnosno("baba meca 2");
//					JPA.getUtil().runInTransaction(() ->this.doc = docDao.save(this.doc, false, null, null, sd));
//					LOGGER.info("--OK2-- dao.findById(this.doc.getId())ID="+this.doc.getId()+ " "+this.doc.getPnr());
//					String objectToXml = JAXBHelper.objectToXml(this.doc, true);
//					LOGGER.info(objectToXml);
//				} catch (BaseException e) {
//					LOGGER.error(e.getMessage(), e);
//					fail(e.getMessage());
//				} catch (JAXBException e) {
//					LOGGER.error(e.getMessage(), e);
//					fail(e.getMessage());
//				} catch (IOException e) {
//					LOGGER.error(e.getMessage(), e);
//					fail(e.getMessage());
//				}
//
//	}

}

package com.ib.urireg.udostDocs;

import com.ib.urireg.system.SystemData;
import com.ib.urireg.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.SystemClassifDAO;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SysClassifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Помощен клас, който върши сложната работа по изграждането на дървовидна структура от класификации
 * и превръщането ѝ в стринг, който да може да се запише в уърдовски документ.
 * Подават се само кодът на класификацията и желаните значения и се извиква {@link TreeBuilder#buildTree()}.
 * Класът изгражда всичко и подредените значения могат да се вземат с {@link TreeBuilder#getResultList()},
 * а съответните нива за табулиране с {@link TreeBuilder#getLevels()}.
 *
 * @author n.kanev
 */
public class TreeBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(TreeBuilder.class);
	private static final String DB_ERROR_MSG = "Грешка при работа с базата";

	private int codeClassif;
	private List<Integer> izbraniZnachenia;
	private List<SystemClassif> completeClassif;
	private List<SystemClassif> extractedZnachenia;
	private SystemClassifDAO dao;
	private final UserData userData;
	private final SystemData systemData;

	public TreeBuilder(UserData userData, SystemData systemData, int codeClassif, List<Integer> izbraniZnachenia) {
		this.userData = userData;
		this.systemData = systemData;
		this.codeClassif = codeClassif;
		this.izbraniZnachenia = izbraniZnachenia;
	}

	// тук идват подредените готови значения
	private List<SystemClassif> resultList;
	// на всеки елемент се записва колко нива навътре в дървото е съответният елемент в resultList
	private List<Integer> levels;

	public void buildTree() {
		try {
			this.completeClassif = this.systemData.getSysClassification(this.codeClassif, new Date(), 1);
			this.extractedZnachenia = new ArrayList<>();

			for(Integer codeZnachenie : this.izbraniZnachenia) {
				this.addToSelectedZnachenia(codeZnachenie);
			}

			this.resultList = new ArrayList<>();
			this.levels = new ArrayList<>();

			int theParentOfParents = this.extractedZnachenia
					.stream()
					.min((s1, s2) -> s1.getCodeParent() - s2.getCodeParent())
					.map(SystemClassif::getCodeParent)
					.orElse(0);
			this.addToResultList(theParentOfParents, 0);
		}
		catch(DbErrorException e) {
			LOGGER.error(DB_ERROR_MSG, e);
		}

	}

	private void addToSelectedZnachenia(Integer codeZnach) throws DbErrorException {
		SystemClassif c11 = null;

		try {
			c11 = this.getDao().findByCode(this.codeClassif, codeZnach, true);
		}
		finally {
			JPA.getUtil().closeConnection();
		}
		List<SystemClassif> parents = SysClassifUtils.getParents(this.completeClassif, c11);

		for(SystemClassif s : parents) {
			boolean contained = false;
			for(SystemClassif s1 : this.extractedZnachenia) {
				if(s1.equals(s)) {
					contained = true;
					break;
				}
			}

			if(!contained) {
				this.extractedZnachenia.add(s);
			}

		}
	}

	private void addToResultList(Integer parentCode, int depthLevel) {
		if(this.extractedZnachenia.isEmpty()) return; // da spre rekursiata

		List<SystemClassif> childToAdd =
				this.extractedZnachenia
					.stream()
					.filter(s -> s.getCodeParent() == parentCode)
					.collect(Collectors.toList());

		for(SystemClassif c : childToAdd) {
			int index = this.extractedZnachenia.indexOf(c);
			this.extractedZnachenia.remove(index); // tova raboti

			int indexWhereToAdd = this.resultList.size();;

			if(this.resultList.isEmpty()) {
				indexWhereToAdd = 0;
			}
			else {
				for(int i = 0; i < this.resultList.size(); i++) {
					if(this.resultList.get(i).getCode() == parentCode) {
						indexWhereToAdd = i + 1;
						// da se proveri sledva6tite elementi dali sa siblings
						int j = indexWhereToAdd;
						while(j < this.resultList.size() && this.resultList.get(j) != null && this.resultList.get(j).getCodeParent() == c.getCodeParent()) {
							j++;
						}
						indexWhereToAdd = j;
						break;
					}
				}
			}

			this.resultList.add(indexWhereToAdd, c);
			this.levels.add(indexWhereToAdd, depthLevel);
		}

		for (SystemClassif newlyAddedChild : childToAdd) {
			addToResultList(newlyAddedChild.getCode(), (depthLevel + 1));
		}
	}

	private SystemClassifDAO getDao() {
		if(this.dao == null) {
			this.dao = new SystemClassifDAO(userData);
		}
		return this.dao;
	}

	public List<SystemClassif> getResultList() {
		return resultList;
	}

	public List<Integer> getLevels() {
		return levels;
	}
}

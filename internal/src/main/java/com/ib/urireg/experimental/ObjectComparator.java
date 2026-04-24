package com.ib.urireg.experimental;

import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.db.dto.Izpit;
import com.ib.urireg.db.dto.Lice;
import com.ib.urireg.system.UriregConstants;
import com.ib.indexui.db.dto.AdmGroup;
import com.ib.indexui.system.Constants;
import com.ib.system.BaseObjectComparator;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;

import java.util.Date;

public class ObjectComparator extends BaseObjectComparator {

	public ObjectComparator(Date oldDate, Date newDate, BaseSystemData sd) {
		super(oldDate, newDate, sd);
	}

	public ObjectComparator(Date oldDate, Date newDate, BaseSystemData sd, Integer lang) {
		super(oldDate, newDate, sd, lang);
	}

	@Override
	protected String formatVal(Object o, String codeClassif, int codeObject, Date dat) {
		//System.out.println("CC=" + codeClassif);

		if (codeObject > 0) {
			String ident = null;
			try {
				Integer id = Integer.parseInt("" + o);

				switch (codeObject) {

					case Constants.CODE_ZNACHENIE_JOURNAL_GROUPUSER:
						AdmGroup group = JPA.getUtil().getEntityManager().find(AdmGroup.class, id);
						if (group != null) {
							ident = group.getIdentInfo();
						}
						break;

					case UriregConstants.CODE_ZNACHENIE_JOURNAL_DOC:
						Doc doc = JPA.getUtil().getEntityManager().find(Doc.class, id);
						if (doc != null) {
							ident = doc.getIdentInfo();
						}
						break;

					case UriregConstants.CODE_ZNACHENIE_JOURNAL_IZPIT:
						Izpit izpit = JPA.getUtil().getEntityManager().find(Izpit.class, id);
						if (izpit != null) {
							ident = izpit.getIdentInfo();
						}
						break;

					case UriregConstants.CODE_ZNACHENIE_JOURNAL_LICE:
						Lice lice = JPA.getUtil().getEntityManager().find(Lice.class, id);
						if (lice != null) {
							ident = lice.getIdentInfo();
						}
						break;


					default:
						break;
				}

				if (ident == null) {
					//Правим още един опит през журнала
					SystemJournal j = (SystemJournal) JPA.getUtil().getEntityManager().createQuery("from SystemJournal where codeObject =:co and idObject = :io and codeAction = :ca").setParameter("co", codeObject).setParameter("io", id).setParameter("ca", SysConstants.CODE_DEIN_IZTRIVANE).getSingleResult();
					if (j != null) {
						ident = j.getIdentObject();
					}
				}

				if (ident == null) {
					return "Id= " + id;
				} else {
					return ident + "(Id= " + id + ")";
				}

			} catch (Exception e) {
				return ident + " (Грешка при идентификация)";
			}


		} else {
			if (codeClassif == null || codeClassif.equalsIgnoreCase("none")) {
				return fromatSimpleVal(o);
			} else {
				return decodeVal(o, codeClassif, dat, lang);
			}
		}
	}
}




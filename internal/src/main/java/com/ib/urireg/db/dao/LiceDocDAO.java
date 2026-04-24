package com.ib.urireg.db.dao;

import com.ib.urireg.db.dto.LiceDoc;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;

public class LiceDocDAO extends AbstractDAO<LiceDoc> {

	public LiceDocDAO(ActiveUser user) {
		super(LiceDoc.class, user);
	}
}

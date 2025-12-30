package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents the combined operational status of a router interface.
 * <p>
 * Interface status consists of two components:
 * <ul>
 *   <li><b>Administrative state</b> - controlled by configuration (up/admin down)</li>
 *   <li><b>Link state</b> - physical layer status (up/down)</li>
 * </ul>
 * <p>
 * An interface is only fully operational when both states are UP.
 */
@Getter
@EqualsAndHashCode
public class InterfaceStatus {
	private final AdminState admin;
	private final LinkState link;

	/**
	 * Creates an interface status with both states UP (default operational state).
	 */
	public InterfaceStatus() {
		this.admin = AdminState.UP;
		this.link = LinkState.UP;
	}

	/**
	 * Creates an interface status with specified states.
	 *
	 * @param admin the administrative state
	 * @param link the physical link state
	 */
	public InterfaceStatus(AdminState admin, LinkState link) {
		this.admin = admin;
		this.link = link;
	}

	/**
	 * Parses an interface status from character codes.
	 *
	 * @param admin Admin state character code
	 * @param link Link state character code
	 * @return InterfaceStatus object
	 */
	public static InterfaceStatus fromChars(char admin, char link) {
		return new InterfaceStatus(
				AdminState.fromCode(admin),
				LinkState.fromCode(link)
		);
	}

	@Override
	public String toString() {
		return admin.getCode() + "/" + link.getCode();
	}
}
